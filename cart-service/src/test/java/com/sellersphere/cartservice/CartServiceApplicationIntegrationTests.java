package com.sellersphere.cartservice;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"security.jwt.issuer=issuer",
		"security.jwt.secret=secret",
		"aws.region=us-west-2",
		"aws.access-key-id=access",
		"aws.secret-access-key=secret"
})
class CartServiceApplicationIntegrationTests {

	@Container
	static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.yaml"), new File("./compose.yaml"))
			.withExposedService("dynamodb", 8000)
			.withExposedService("user-service", 8080)
			.withExposedService("mailhog", 8025);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry){
		registry.add("dynamodb.url", () -> "http://" + COMPOSE.getServiceHost("dynamodb", 8000)
			 + ":" + COMPOSE.getServicePort("dynamodb", 8000));
	}

	@LocalServerPort
	int cartService;
	int userService, mailhog;

	@Test
	@DisplayName("Only authenticated users can access the cart")
	void onlyAuthenticatedUsersCanAccessTheCart() {
		given().port(cartService)
				.when().get("/cart")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	@DisplayName("Authenticated users can access their cart")
	void authenticatedUsersCanAccessTheirCart() {
		var userAccessToken = createUserAccessToken("bob@bmail.com");

		assertThat(given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.when().get("/cart")
				.then().statusCode(200)
				.extract().jsonPath().getList("$")).isEmpty();

	}

	@Test
	@DisplayName("Authenticated users can add items to their carts")
	void authenticatedUsersCanAddItemsToTheirCarts() {
		var userAccessToken = createUserAccessToken("mike@mmail.com");

		var cartItem = new CartItem(VALID_PRODUCT_ID, 5);
		given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.contentType(ContentType.JSON).body(cartItem)
				.when().patch("/cart")
				.then().statusCode(200);

		assertThat(given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.when().get("/cart")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", CartItem.class))
				.containsExactly(cartItem);
	}

	@Test
	@DisplayName("Specifing 0 as the quantity of cartItem, removes the item from the cart")
	void specifing0AsTheQuantityOfCartItemRemovesTheItemFromTheCart() {
		var userAccessToken = createUserAccessToken("alise@amail.com");

		given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.contentType(ContentType.JSON).body(new CartItem(VALID_PRODUCT_ID, 10))
				.when().patch("/cart")
				.then().statusCode(200);

		given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.contentType(ContentType.JSON).body(new CartItem(VALID_PRODUCT_ID, 0))
				.when().patch("/cart")
				.then().statusCode(200);

		assertThat(given().port(cartService).header("Authorization", "Bearer ".concat(userAccessToken))
				.when().get("/cart")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", CartItem.class))
				.isEmpty();
	}

	static final String VALID_PRODUCT_ID = "qwertyuiopasdfghjklzxcvb";

	@Value("${security.jwt.issuer}")
	String jwtIssuer;
	@Value("${security.jwt.secret}")
	String jwtSecret;

	private String createUserAccessToken(String userMail){
		return JWT.create()
				.withIssuer(jwtIssuer)
				.withClaim("Scope", "user")
				.withExpiresAt(Instant.now().plusSeconds(60))
				.withSubject("USER#".concat(userMail))
				.sign(Algorithm.HMAC256(jwtSecret));
	}

	@BeforeEach
	void setPorts(){
		userService = COMPOSE.getServicePort("user-service", 8080);
		mailhog = COMPOSE.getServicePort("mailhog", 8025);
	}


}
