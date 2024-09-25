package com.streamcart.userservice;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public final class UserService {

    private static final AttributeValue NOT_VERIFIED = AttributeValue.fromS("NOT_VERIFIED");
    private static final AttributeValue METADATA = AttributeValue.fromS("METADATA");

    private final DynamoDbClient dynamoDB;
    private final Supplier<Instant> clock;
    private final SecureRandom secureRandom;
    private final ObservationRegistry observationRegistry;

    @Autowired
    public UserService(DynamoDbClient dynamoDB, ObservationRegistry observationRegistry) {
        this(dynamoDB, Instant::now, observationRegistry);
    }

    public UserService(DynamoDbClient dynamoDB, Supplier<Instant> clock, ObservationRegistry observationRegistry) {
        this.dynamoDB = dynamoDB;
        this.clock = clock;
        this.observationRegistry = observationRegistry;
        this.secureRandom = new SecureRandom();
    }

    public String registerUser(UserCredentials credentials) throws UserAlreadyExistsException {
        return Observation.start("user-service.registerUser", observationRegistry).observeChecked(() -> {
            var verificationCode = String.valueOf(secureRandom.nextInt(111_111, 999_999));
            var expiresAt = String.valueOf(clock.get().plus(10, ChronoUnit.MINUTES).getEpochSecond());
            try {
                dynamoDB.putItem(PutItemRequest.builder()
                        .tableName("USERS")
                        .item(Map.of("PK", AttributeValue.fromS("USER#".concat(credentials.email())),
                                "SK", METADATA,
                                "PASS", AttributeValue.fromS(credentials.password()),
                                "DEACTIVED", NOT_VERIFIED,
                                "VERIFICATION_CODE", AttributeValue.fromS(verificationCode),
                                "EXPIRES_AT", AttributeValue.fromN(expiresAt)))
                        .conditionExpression("attribute_not_exists(PK) OR DEACTIVED = :notVerified")
                        .expressionAttributeValues(Map.of(":notVerified", NOT_VERIFIED))
                        .build());
                return verificationCode;
            } catch (ConditionalCheckFailedException e) {
                throw new UserAlreadyExistsException("The user email %s is already taken by a verified account".formatted(credentials.email()));
            }
        });
    }

    public Boolean verifyUserAccount(AccountVerificationPayload payload) {
        return Observation.start("user-service.verifyUserAccount", observationRegistry).observe(() -> {
            try {
                var now = String.valueOf(clock.get().getEpochSecond());
                dynamoDB.updateItem(UpdateItemRequest.builder()
                        .tableName("USERS")
                        .key(Map.of("PK", AttributeValue.fromS("USER#".concat(payload.email())), "SK", METADATA))
                        .updateExpression("REMOVE DEACTIVED, VERIFICATION_CODE, EXPIRES_AT")
                        .conditionExpression("attribute_exists(PK) AND DEACTIVED = :notVerified AND " +
                                "VERIFICATION_CODE = :verificationCode AND EXPIRES_AT > :now")
                        .expressionAttributeValues(Map.of(
                                ":notVerified", NOT_VERIFIED,
                                ":verificationCode", AttributeValue.fromS(payload.verificationCode()),
                                ":now", AttributeValue.fromN(now)))
                        .build());
                return true;
            } catch (ConditionalCheckFailedException e) {
                return false;
            }
        });
    }

    public Optional<UserProfile> loginUser(UserCredentials credentials) {
        return Observation.start("user-service.loginUser", observationRegistry).observe(() -> {
            var userId = "USER#".concat(credentials.email());
            var response = dynamoDB.getItem(GetItemRequest.builder()
                    .tableName("USERS")
                    .consistentRead(true)
                    .key(Map.of("PK", AttributeValue.fromS(userId), "SK", METADATA))
                    .projectionExpression("PASS, DEACTIVED")
                    .build()).item();
            AttributeValue pass;
            if (response.containsKey("DEACTIVED") ||
                    ((pass = response.get("PASS")) == null ||
                            !credentials.password().equals(pass.s()))) return Optional.empty();
            return Optional.of(new UserProfile(userId));
        });
    }
}
