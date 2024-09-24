package com.streamcart.userservice;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.restassured.RestAssured.*;
import static java.util.function.Predicate.not;

@Testcontainers
@Import(IntegrationConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceApplicationIntegrationTests {

    @Container
    static final ComposeContainer COMPOSE = new ComposeContainer(new File("compose.yaml"))
            .withExposedService("mailhog", 1025)
            .withExposedService("mailhog", 8025)
            .withExposedService("dynamodb-local", 8000);

    static Supplier<Instant> CLOCK = Instant::now;

    @Test
    @DisplayName("Complete Signup -> Verify -> Login -> Logout flow")
    void completeSignupVerifyLoginLogoutFlow() {
        var user = new UserCredentials("bob@bmail.com", "password");
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(HttpStatus.OK.value());
        var verificationCode = findVerificationCodeOf(user.email());
        given().contentType(ContentType.JSON).body(new AccountVerificationPayload(user.email(), verificationCode))
                .when().patch("/users/verify")
                .then().statusCode(HttpStatus.OK.value());

        var refreshToken = given().contentType(ContentType.JSON).body(user)
                .when().post("/users/login")
                .then().statusCode(HttpStatus.OK.value())
                .extract().header("X-REFRESH-TOKEN");

        given().header("X-REFRESH-TOKEN", refreshToken)
                .when().delete("/users/logout")
                .then().statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("User account must be verified to login")
    void userAccountMustBeVerifiedToLogin() {
        var user = new UserCredentials("alise@amail.com", "alise12345");
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(200);

        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/login")
                .then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("User must specify the current credentials to login")
    void userMustSpecifyTheCurrentCredentialsToLogin() {
        var user = registerAndVerifyAccount("quan@qmail", "quanthebest");
        given().contentType(ContentType.JSON).body(new UserCredentials(user.email(), "incorrect"))
                .when().post("/users/login")
                .then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Registering a not verified account changes the verification code")
    void registeringANotVerifiedAccountChangesTheVerificationCode() {
        var user = new UserCredentials("mike@mmail.com", "mikethebest");
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(200);
        var firstVerificationCode = findVerificationCodeOf(user.email());
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(200);
        var secVerificationCode = allVerificationCodesOf(user.email())
                .filter(not(firstVerificationCode::equals))
                .findAny().orElseThrow();
        given().contentType(ContentType.JSON).body(new AccountVerificationPayload(user.email(), firstVerificationCode))
                .when().patch("/users/verify")
                .then().statusCode(HttpStatus.CONFLICT.value());
        given().contentType(ContentType.JSON).body(new AccountVerificationPayload(user.email(), secVerificationCode))
                .when().patch("/users/verify")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("The VerificationCode expires after 10 Minutes")
    void theVerificationCodeExpiresAfter10Minutes() {
        var now = Instant.now();
        CLOCK = () -> now;
        var user = new UserCredentials("john@jmail.com", "12345678");
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(200);
        var verificationCode = findVerificationCodeOf(user.email());
        CLOCK = () -> now.plus(10, ChronoUnit.MINUTES);
        given().contentType(ContentType.JSON).body(new AccountVerificationPayload(user.email(), verificationCode))
                .when().patch("/users/verify")
                .then().statusCode(HttpStatus.CONFLICT.value());
        CLOCK = Instant::now;
    }

    @Test
    @DisplayName("Email is already taken")
    void emailIsAlreadyTaken() {
        var user = registerAndVerifyAccount("paul@pmail.com", "paul5555");

        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Refresh Tokens")
    void refreshTokens() {
        var user = registerAndVerifyAccount("bane@bmail.com", "bane9999");
        var refreshToken = given().contentType(ContentType.JSON).body(user)
                .when().post("/users/login")
                .then().extract().header("X-REFRESH-TOKEN");

        for (int i = 0; i < 5; i++) {
            refreshToken = given().header("X-REFRESH-TOKEN", refreshToken)
                    .when().post("/users/refresh")
                    .then().statusCode(200)
                    .extract().header("X-REFRESH-TOKEN");
        }
    }

    @Test
    @DisplayName("Detect Refresh token reuse")
    void detectRefreshTokenReuse() {
        var user = registerAndVerifyAccount("hana@hmail.com", "password");

        var refreshToken1 = given().contentType(ContentType.JSON).body(user)
                .when().post("/users/login")
                .then().statusCode(200).extract().header("X-REFRESH-TOKEN");

        // ---- Attacker steals and uses refreshToken1
        var refreshToken2 = given().header("X-REFRESH-TOKEN", refreshToken1)
                .when().post("/users/refresh")
                .then().statusCode(200)
                .extract().header("X-REFRESH-TOKEN");

        var refreshToken3 = given().header("X-REFRESH-TOKEN", refreshToken2)
                .when().post("/users/refresh")
                .then().statusCode(200)
                .extract().header("X-REFRESH-TOKEN");

        // ---- The user uses its refresh token
        given().header("X-REFRESH-TOKEN", refreshToken1)
                .when().post("/users/refresh")
                .then().statusCode(HttpStatus.FORBIDDEN.value());
        // ---- the system identifies a refresh token reuse and invalides the session

        given().header("X-REFRESH-TOKEN", refreshToken3)
                .when().post("/users/refresh")
                .then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("When a user logouts its session is invalided")
    void whenAUserLogoutsItsSessionIsInvalided() {
        var user = registerAndVerifyAccount("george@gmail.com", "georgethebest");

        var refreshToken = given().contentType(ContentType.JSON).body(user)
                .when().post("/users/login")
                .then().statusCode(200)
                .extract().header("X-REFRESH-TOKEN");

        given().header("X-REFRESH-TOKEN", refreshToken)
                .when().delete("/users/logout")
                .then().statusCode(200);

        given().header("X-REFRESH-TOKEN", refreshToken)
                .when().post("/users/refresh")
                .then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    private @NotNull UserCredentials registerAndVerifyAccount(String mail, String bane9999) {
        var user = new UserCredentials(mail, bane9999);
        given().contentType(ContentType.JSON).body(user)
                .when().post("/users/register")
                .then().statusCode(200);
        var verificationCode = findVerificationCodeOf(mail);
        given().contentType(ContentType.JSON).body(new AccountVerificationPayload(mail, verificationCode))
                .when().patch("/users/verify")
                .then().statusCode(200);
        return user;
    }


    private String findVerificationCodeOf(String userEmail) {
        return allVerificationCodesOf(userEmail).iterator().next();
    }

    private Stream<String> allVerificationCodesOf(String userEmail){
        RestAssured.port = mailhogApiPort;
        var jsonPath = get("/api/v1/messages").then().statusCode(200).extract().jsonPath();
        RestAssured.port = springBootPort;
        return jsonPath.param("email", userEmail)
                .<List<String>>get("findAll(msg -> msg.Content.Headers.To[0] == email).Content.Body")
                .stream()
                .map(body -> {
            int offset = body.lastIndexOf("VerificationCode: ") + "VerificationCode: ".length();
            return body.substring(offset, offset + 6);
        });
    }

    @LocalServerPort
    int springBootPort;
    int mailhogApiPort;

    @BeforeEach
    void setRestAssured() {
        RestAssured.port = springBootPort;
        mailhogApiPort = COMPOSE.getServicePort("mailhog", 8025);
    }

    @BeforeAll
    static void ensureDynamoDBReady() throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(2));
    }

}
