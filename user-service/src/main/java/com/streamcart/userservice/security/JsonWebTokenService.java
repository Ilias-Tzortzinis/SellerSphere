package com.streamcart.userservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public final class JsonWebTokenService {

    public static final String ISSUER = "https://streamcart.com";

    private final JWTVerifier verifier;
    private final Algorithm jwtAlgorithm;
    private final DynamoDbClient dynamoDB;
    private final SecureRandom secureRandom;
    private final int maxRefreshesPerSession;
    private final ObservationRegistry observationRegistry;
    private final Duration accessTokenDuration, refreshTokenDuration;

    public JsonWebTokenService(DynamoDbClient dynamoDB, ObservationRegistry observationRegistry,
                               @Value("${security.jwt.secret-key}") String secretKey,
                               @Value("${security.access-token.duration}") String accessTokenDurationStr,
                               @Value("${security.refresh-token.duration}") String refreshTokenDurationStr) {
        this.observationRegistry = observationRegistry;
        this.accessTokenDuration = Duration.parse(accessTokenDurationStr);
        this.refreshTokenDuration = Duration.parse(refreshTokenDurationStr);
        this.jwtAlgorithm = Algorithm.HMAC256(secretKey);
        this.dynamoDB = dynamoDB;
        this.verifier = JWT.require(jwtAlgorithm)
                .withIssuer(ISSUER)
                .withClaimPresence("session_id")
                .withClaimPresence("refresh_n")
                .withClaim("scope", "user")
                .build();
        secureRandom = new SecureRandom();
        maxRefreshesPerSession = (int) (refreshTokenDuration.toSeconds() / accessTokenDuration.toSeconds());
    }

    public AccessAndRefreshToken createNewSession(String userId) {
        return Observation.start("user-service.jwt.createNewSession", observationRegistry).lowCardinalityKeyValue("userId", userId).observe(() -> {
            var now = Instant.now();
            var sessionId = String.valueOf(secureRandom.nextLong());
            var refreshTokenExpiration = now.plus(refreshTokenDuration);
            var accessToken = createToken(userId, now, now.plus(accessTokenDuration), sessionId, 0);
            var refreshToken = createToken(userId, now, refreshTokenExpiration, sessionId, 0);
            dynamoDB.putItem(PutItemRequest.builder()
                    .tableName("AUTH_SESSIONS")
                    .item(Map.of("SUBJECT", AttributeValue.fromS(userId),
                            "SESSION_ID", AttributeValue.fromS(sessionId),
                            "REFRESH_N", AttributeValue.fromS("0"),
                            "EXPIRES_AT", AttributeValue.fromN(String.valueOf(refreshTokenExpiration.getEpochSecond()))))
                    .build());
            return new AccessAndRefreshToken(accessToken, refreshToken);
        });
    }

    public AccessAndRefreshToken refreshTokens(String refreshToken) throws InvalidRefreshTokenException {
        return Observation.start("user-service.jwt.refreshTokens", observationRegistry).observeChecked(() -> {
            DecodedJWT jwt;
            try {
                jwt = verifier.verify(refreshToken);
            } catch (JWTVerificationException e) {
                throw new InvalidRefreshTokenException(e);
            }
            var sessionId = jwt.getClaim("session_id").asString();
            int refreshN = jwt.getClaim("refresh_n").asInt();
            var userId = jwt.getSubject();
            if (refreshN >= maxRefreshesPerSession) {
                deleteSession(userId, sessionId);
                throw new InvalidRefreshTokenException("Max number of refreshes reached for session of user with id: " + userId);
            }
            try {
                var newRefreshN = refreshN + 1;
                dynamoDB.updateItem(UpdateItemRequest.builder()
                        .tableName("AUTH_SESSIONS")
                        .key(Map.of("SUBJECT", AttributeValue.fromS(userId), "SESSION_ID", AttributeValue.fromS(sessionId)))
                        .updateExpression("SET REFRESH_N = :newRefreshN")
                        .conditionExpression("REFRESH_N = :expectedRefreshN")
                        .expressionAttributeValues(Map.of(
                                ":newRefreshN", AttributeValue.fromS(String.valueOf(newRefreshN)),
                                ":expectedRefreshN", AttributeValue.fromS(String.valueOf(refreshN))))
                        .build());
                var now = Instant.now();
                var accessToken = createToken(userId, now, now.plus(accessTokenDuration), sessionId, newRefreshN);
                var newRefreshToken = createToken(userId, now, now.plus(refreshTokenDuration), sessionId, newRefreshN);
                return new AccessAndRefreshToken(accessToken, newRefreshToken);
            } catch (ConditionalCheckFailedException e) {
                deleteSession(userId, sessionId);
                throw new InvalidRefreshTokenException("RefreshToken was reused in session with subject: ".concat(userId));
            }
        });
    }

    public void deleteSession(String refreshToken) throws JWTVerificationException {
        var jwt = verifier.verify(refreshToken);
        deleteSession(jwt.getSubject(), jwt.getClaim("session_id").asString());
    }

    private void deleteSession(String subject, String sessionId) {
        Observation.start("user-service.jwt.deleteSession", observationRegistry).observe(() -> {
            dynamoDB.deleteItem(DeleteItemRequest.builder()
                    .tableName("AUTH_SESSIONS")
                    .key(Map.of("SUBJECT", AttributeValue.fromS(subject), "SESSION_ID", AttributeValue.fromS(sessionId)))
                    .build());
        });
    }

    private String createToken(String userId, Instant now, Instant expiresAt, String sessionId, int refreshN) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim("session_id", sessionId)
                .withClaim("refresh_n", refreshN)
                .withClaim("scope", "user")
                .withSubject(userId)
                .sign(jwtAlgorithm);
    }
}
