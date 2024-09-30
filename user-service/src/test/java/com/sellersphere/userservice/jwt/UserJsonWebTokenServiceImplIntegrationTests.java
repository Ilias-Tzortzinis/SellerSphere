package com.sellersphere.userservice.jwt;

import com.sellersphere.userservice.UserServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.File;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class UserJsonWebTokenServiceImplIntegrationTests {

    @Container
    static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.yaml"))
            .withExposedService("dynamodb", 8000);

    final Clock clock;
    final DynamoDbClient dynamoDB;
    final UserJsonWebTokenServiceImpl jsonWebTokenService;


    UserJsonWebTokenServiceImplIntegrationTests(){
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.instant()).thenReturn(Instant.now());
        dynamoDB = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .endpointOverride(URI.create("http://localhost:" + COMPOSE.getServicePort("dynamodb", 8000)))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("access", "secret")))
                .build();
        jsonWebTokenService = new UserJsonWebTokenServiceImpl("secret", "issuer",
                "PT15M", "P1D", dynamoDB, clock);
    }

    @Test
    @DisplayName("InvalidRefreshToken exception if the refreshToken is invalid jwt")
    void invalidRefreshTokenExceptionIfTheRefreshTokenIsInvalidJwt() {
        String invalidJwt = "<invalid>";

        assertThrows(InvalidRefreshTokenException.class, () -> jsonWebTokenService.refreshTokens(invalidJwt));
    }

    @Test
    @DisplayName("InvalidRefreshToken on refreshToken reuse")
    void invalidRefreshTokenOnRefreshTokenReuse() {
        var appRefreshToken = jsonWebTokenService.createUserSession("USER#victim@vmail.com").refreshToken();

        // ==== Hacker Steals appRefreshToken
        var hackerRefreshToken = assertDoesNotThrow(() -> jsonWebTokenService.refreshTokens(appRefreshToken)).refreshToken();

        // ==== App uses the refresh token
        assertThrows(InvalidRefreshTokenException.class, () -> jsonWebTokenService.refreshTokens(appRefreshToken));

        // ==== Hacker refersh token is also invalidated
        assertThrows(InvalidRefreshTokenException.class, () -> jsonWebTokenService.refreshTokens(hackerRefreshToken));
    }
}
