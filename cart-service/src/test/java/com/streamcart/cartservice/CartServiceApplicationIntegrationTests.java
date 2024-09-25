package com.streamcart.cartservice;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.File;
import java.net.URI;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Import(IntegrationTestsConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartServiceApplicationIntegrationTests {

	@Container
	static final ComposeContainer COMPOSE = new ComposeContainer(new File("compose.yaml"))
			.withExposedService("dynamodb", 8000)
			.withExposedService("user-service", 8080)
			.withExposedService("mailhog", 8025);

	@Test
	@DisplayName("Cart can be accessed only by Authorized Users")
	void cartCanBeAccessedOnlyByAuthorizedUsers() {
		when().get("/cart").then().statusCode(HttpStatus.UNAUTHORIZED.value());
		when().patch("/cart").then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	@DisplayName("Authorized Users can access and modify their cart")
	void authorizedUsersCanAccessAndModifyTheirCart() {
		var accessToken = authorizeNewUser("bob@bmail.com", "bobthebest");

		var cartItem = new CartItem("12345", "Apple", 5);
		given().header("Authorization", "Bearer ".concat(accessToken))
				.contentType(ContentType.JSON).body(cartItem)
				.when().patch("/cart")
				.then().statusCode(200);

		var cart = given().header("Authorization", "Bearer ".concat(accessToken))
				.when().get("/cart")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", CartItem.class);

		assertThat(cart).containsExactly(cartItem);

		given().header("Authorization", "Bearer ".concat(accessToken))
				.contentType(ContentType.JSON).body(new CartItem(cartItem.productId(), cartItem.name(), 0))
				.when().patch("/cart")
				.then().statusCode(200);

		cart = given().header("Authorization", "Bearer ".concat(accessToken))
				.when().get("/cart")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", CartItem.class);

		assertThat(cart).isEmpty();
	}


	private String authorizeNewUser(String email, String pass){
		String credentials = """
				{ "email": "%s", "password": "%s" }
				""".formatted(email, pass);
		// Register User
		RestAssured.port = userServicePort;
		given().contentType(ContentType.JSON).body(credentials)
				.when().post("/users/register")
				.then().statusCode(200);

		// Find Email verification code
		RestAssured.port = mailhogPort;
		String emailBody = when().get("/api/v1/messages").then().statusCode(200)
				.extract().jsonPath()
				.param("email", email).get("find(msg -> msg.Content.Headers.To[0] == email).Content.Body");
		// Extract the verification code from the email body
		var offset = emailBody.lastIndexOf("VerificationCode: ") + "VerificationCode: ".length();
		var verificationCode = emailBody.substring(offset, offset + 6);

		// Verify the user
		RestAssured.port = userServicePort;
		given().contentType(ContentType.JSON).body("""
				{ "email": "%s", "verificationCode": "%s" }
				""".formatted(email, verificationCode))
				.when().patch("/users/verify")
				.then().statusCode(200);

		// Login the user
		var accessToken = given().contentType(ContentType.JSON).body(credentials)
				.when().post("/users/login")
				.then().statusCode(200)
				.extract().header("X-ACCESS-TOKEN");
		RestAssured.port = cartServicePort;
		return accessToken;
	}

	@LocalServerPort
	int cartServicePort;
	int userServicePort, mailhogPort;

	@BeforeEach
	void setPorts(){
		userServicePort = COMPOSE.getServicePort("user-service", 8080);
		mailhogPort = COMPOSE.getServicePort("mailhog", 8025);
		RestAssured.port = cartServicePort;
	}

}
