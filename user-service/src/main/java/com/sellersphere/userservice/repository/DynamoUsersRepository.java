package com.sellersphere.userservice.repository;

import com.sellersphere.userservice.UserEmailAlreadyExistException;
import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserProfile;
import com.sellersphere.userservice.data.UserSignupVerification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public final class DynamoUsersRepository implements UsersRepository {

    public static final AttributeValue UNVERIFIED = AttributeValue.fromS("UNVERIFIED");
    public static final AttributeValue METADATA = AttributeValue.fromS("METADATA");

    private final Clock clock;
    private final DynamoDbClient dynamoDB;
    private final PasswordEncoder passwordEncoder;
    private final Duration signupVerificationExpiration;

    @Autowired
    public DynamoUsersRepository(DynamoDbClient dynamoDB,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${signup-verification-expiration:PT10M}") String signupVerificationExpiration) {
        this(Clock.systemUTC(), dynamoDB, passwordEncoder, signupVerificationExpiration);
    }

    public DynamoUsersRepository(Clock clock, DynamoDbClient dynamoDB,
                                 PasswordEncoder passwordEncoder, String signupVerificationExpiration) {
        this.clock = clock;
        this.dynamoDB = dynamoDB;
        this.passwordEncoder = passwordEncoder;
        this.signupVerificationExpiration = Duration.parse(signupVerificationExpiration);
    }

    @Override
    public void signupUser(UserCredentials credentials, String verificationCode) throws UserEmailAlreadyExistException {
        var passwordEncoded = passwordEncoder.encode(credentials.password());
        var expiresAt = String.valueOf(clock.instant().plus(signupVerificationExpiration).getEpochSecond());
        try {
            dynamoDB.putItem(PutItemRequest.builder()
                    .tableName("Users")
                    .conditionExpression("attribute_not_exists(PK) OR DisabledReson = :unverified")
                    .item(Map.of(
                            "PK", AttributeValue.fromS("USER#".concat(credentials.email())),
                            "SK", METADATA,
                            "Password", AttributeValue.fromS(passwordEncoded),
                            "DisabledReason", UNVERIFIED,
                            "VerificationCode", AttributeValue.fromS(verificationCode),
                            "Expiration", AttributeValue.fromN(expiresAt)
                    )).expressionAttributeValues(Map.of(":unverified", UNVERIFIED)).build());
        } catch (ConditionalCheckFailedException e){
            throw new UserEmailAlreadyExistException();
        }

    }

    @Override
    public boolean verifySignupUser(UserSignupVerification signupVerification) {
        try {
            var userId = "USER#".concat(signupVerification.email());
            dynamoDB.updateItem(UpdateItemRequest.builder()
                    .tableName("Users")
                    .key(Map.of("PK", AttributeValue.fromS(userId), "SK", METADATA))
                    .updateExpression("REMOVE DisabledReason, VerificationCode, Expiration")
                    .conditionExpression("attribute_exists(PK) AND DisabledReason = :unverified")
                    .expressionAttributeValues(Map.of(":unverified", UNVERIFIED))
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e){
            return false;
        }
    }

    @Override
    public Optional<UserProfile> loginUser(UserCredentials credentials) {
        var userId = "USER#".concat(credentials.email());
        var item = dynamoDB.getItem(GetItemRequest.builder()
                .tableName("Users")
                .key(Map.of("PK", AttributeValue.fromS(userId), "SK", METADATA))
                .consistentRead(true)
                .projectionExpression("Password, DisabledReason")
                .build()).item();
        if (item.containsKey("DisabledReason") || !passwordMatches(item.get("Password"), credentials)){
            return Optional.empty();
        }

        return Optional.of(new UserProfile(userId, credentials.email()));
    }

    private boolean passwordMatches(AttributeValue encodedPassword, UserCredentials credentials){
        if (encodedPassword == null || encodedPassword.s() == null) return false;
        return passwordEncoder.matches(credentials.password(), encodedPassword.s());
    }
}
