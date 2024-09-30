package com.sellersphere.userservice;

import com.sellersphere.userservice.data.UserCredentials;
import com.sellersphere.userservice.data.UserSignupData;
import com.sellersphere.userservice.data.UserSignupVerification;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static io.restassured.RestAssured.given;

@Testcontainers
@TestPropertySource(properties = {"security.jwt.secret=secret", "aws.region=us-west-2", "aws.access-key-id=access", "aws.secret-access-key=secret"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceIntegrationTests {

	@Container
	static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.yaml"))
			.withExposedService("dynamodb", 8000)
			.withExposedService("mailhog", 1025)
			.withExposedService("mailhog", 8025);

	@DynamicPropertySource
	static void testProperties(DynamicPropertyRegistry registry){
		registry.add("spring.mail.host", () -> COMPOSE.getServiceHost("mailhog", 1025));
		registry.add("spring.mail.port", () -> COMPOSE.getServicePort("mailhog", 1025));
		registry.add("dynamodb.uri", () -> "http://" + COMPOSE.getServiceHost("dynamodb", 8000) + ":" +
				COMPOSE.getServicePort("dynamodb", 8000));

		registry.add("mailhog.port", () -> COMPOSE.getServicePort("mailhog", 8025));
	}

	@LocalServerPort
	int userService;
	@Value("${mailhog.port}")
	int mailhogPort;

	@Test
	@DisplayName("User register, verify, login flow")
	void userRegisterVerifyLoginFlow() {
		var credentials = new UserCredentials("bob@bmail.com", "bobthebest");
		given().port(userService).contentType(ContentType.JSON).body(credentials)
				.when().post("/users/signup")
				.then().statusCode(HttpStatus.CREATED.value());

		var verificationCode = verificationCodeOf(credentials.email());

		given().port(userService).contentType(ContentType.JSON).body(new UserSignupVerification(credentials.email(), verificationCode))
				.when().patch("/users/verify/signup")
				.then().statusCode(200);

		given().port(userService).contentType(ContentType.JSON).body(credentials)
				.when().post("/users/login")
				.then().statusCode(200);
	}

	@Test
	@DisplayName("A not registered user cannot login")
	void aNotRegisteredUserCannotLogin() {
		var unknown = new UserCredentials("unknown@mail.com", "unknown");
		given().port(userService).contentType(ContentType.JSON).body(unknown)
				.when().post("/users/login")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	@DisplayName("A not verified user cannot login")
	void aNotVerifiedUserCannotLogin() {
		var notverified = new UserCredentials("notverified@mail.com", "notverified");
		given().port(userService).contentType(ContentType.JSON).body(notverified)
				.when().post("/users/signup")
				.then().statusCode(HttpStatus.CREATED.value());

		given().port(userService).contentType(ContentType.JSON).body(notverified)
				.when().post("/users/login")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}



	private String verificationCodeOf(String userEmail){
		var verificationMailBody = given().port(mailhogPort)
				.when().get("/api/v1/messages")
				.then().statusCode(200)
				.extract().jsonPath()
				.param("email", userEmail).getString("find(msg -> msg.Content.Headers.To[0] == email).Content.Body");
		var offset = verificationMailBody.lastIndexOf("VerificationCode: ") + "VerificationCode: ".length();
        return verificationMailBody.substring(offset, offset + 6);
	}
}
