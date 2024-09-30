package com.sellersphere.userservice.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public final class UserJsonWebTokenServiceImpl implements UserJsonWebTokenService {

    private final Clock clock;
    private final String issuer;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final DynamoDbClient dynamoDB;
    private final SecureRandom secureRandom;
    private final Duration accessTokenDuration, refreshTokenDuration;

    @Autowired
    public UserJsonWebTokenServiceImpl(@Value("${security.jwt.secret}") String secret,
                                       @Value("${security.jwt.issuer:https://seller-sphere.com}") String issuer,
                                       @Value("${security.jwt.access-token-duration:PT15M}") String accessTokenDurationStr,
                                       @Value("${security.jwt.refresh-token-duration:P1D}") String refreshTokenDurationStr,
                                       DynamoDbClient dynamoDB) {
        this(secret, issuer, accessTokenDurationStr, refreshTokenDurationStr, dynamoDB, Clock.systemUTC());
    }

    public UserJsonWebTokenServiceImpl(String secret,
                                       String issuer,
                                       String accessTokenDurationStr,
                                       String refreshTokenDurationStr,
                                       DynamoDbClient dynamoDB,
                                       Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.algorithm = Algorithm.HMAC256(secret);
        this.accessTokenDuration = Duration.parse(accessTokenDurationStr);
        this.refreshTokenDuration = Duration.parse(refreshTokenDurationStr);
        this.issuer = Objects.requireNonNull(issuer);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withClaimPresence("SessionId")
                .withClaimPresence("sub")
                .build();
        secureRandom = new SecureRandom();
        this.dynamoDB = Objects.requireNonNull(dynamoDB);
    }

    @Override
    public AccessAndRefreshTokens createUserSession(String userId){
        var sessionId = String.valueOf(secureRandom.nextLong());
        var now = clock.instant();
        var accessToken = createUserAccessToken(sessionId, userId, now);
        var refreshToken = createUserRefreshToken(sessionId, userId, 1, now);
        dynamoDB.putItem(PutItemRequest.builder()
                .tableName("AuthSessions")
                .item(Map.of("Subject", AttributeValue.fromS(userId),
                        "SessionId", AttributeValue.fromS(sessionId),
                        "RefreshNumber", AttributeValue.fromN("1"),
                        "Expiration", AttributeValue.fromN(String.valueOf(now.plus(refreshTokenDuration).getEpochSecond()))))
                .build());
        return new AccessAndRefreshTokens(accessToken, refreshToken);
    }

    @Override
    public AccessAndRefreshTokens refreshTokens(String refreshToken) throws InvalidRefreshTokenException {
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(refreshToken);
        } catch (JWTVerificationException e) {
            throw new InvalidRefreshTokenException();
        }
        var refreshNumber = jwt.getClaim("RefreshNumber").asInt();
        if (refreshNumber == null) throw new InvalidRefreshTokenException();
        var sessionId = jwt.getClaim("SessionId").asString();
        var userId = jwt.getSubject();
        try {
            dynamoDB.updateItem(UpdateItemRequest.builder()
                            .tableName("AuthSessions")
                            .key(Map.of("Subject", AttributeValue.fromS(userId), "SessionId", AttributeValue.fromS(sessionId)))
                            .updateExpression("SET RefreshNumber = :newRefreshNumber")
                            .conditionExpression("attribute_exists(SessionId) AND RefreshNumber = :expectedRefreshNumber")
                            .expressionAttributeValues(Map.of(
                                    ":newRefreshNumber", AttributeValue.fromN(String.valueOf(refreshNumber + 1)),
                                    ":expectedRefreshNumber", AttributeValue.fromN(String.valueOf(refreshNumber)))
                            ).build());
            var now = clock.instant();
            var accessToken = createUserAccessToken(sessionId, userId, now);
            var newRefreshToken = createUserRefreshToken(sessionId, userId, refreshNumber + 1, now);
            return new AccessAndRefreshTokens(accessToken, newRefreshToken);
        } catch (ConditionalCheckFailedException e) {
            deleteUserSession(userId, sessionId);
            throw new InvalidRefreshTokenException();
        }
    }

    @Override
    public void deleteUserSession(String refreshToken) throws InvalidRefreshTokenException {
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(refreshToken);
        } catch (JWTVerificationException e) {
            throw new InvalidRefreshTokenException();
        }
        if (jwt.getClaim("SessionNumber").isMissing()) {
            throw new InvalidRefreshTokenException();
        }
        deleteUserSession(jwt.getSubject(), jwt.getClaim("SessionId").asString());
    }

    private void deleteUserSession(String userId, String sessionId){
        dynamoDB.deleteItem(DeleteItemRequest.builder()
                .tableName("AuthSessions")
                .key(Map.of("Subject", AttributeValue.fromS(userId), "SessionId", AttributeValue.fromS(sessionId)))
                .build());
    }

    private String createUserAccessToken(String sessionId, String userId, Instant now){
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId)
                .withClaim("SessionId", sessionId)
                .withClaim("Scope", "user")
                .withExpiresAt(now.plus(accessTokenDuration))
                .sign(algorithm);
    }


    private String createUserRefreshToken(String sessionId, String userId, int refreshNumber, Instant now){
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(userId)
                .withClaim("SessionId", sessionId)
                .withClaim("RefreshNumber", refreshNumber)
                .withExpiresAt(now.plus(refreshTokenDuration))
                .sign(algorithm);
    }
}
