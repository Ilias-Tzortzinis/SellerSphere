package com.sellersphere.orderservice;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.mongodb.client.MongoClient;
import com.sellersphere.orderservice.data.OrderDetails;
import com.sellersphere.orderservice.data.OrderItem;
import com.sellersphere.orderservice.data.UserOrderView;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"AWS_REGION=us-west-2",
		"AWS_ACCESS_KEY_ID=access",
		"AWS_SECRET_ACCESS_KEY=secret",
		"security.jwt.secret=secret",
		"kafka.topic=placed-orders"
})
class OrderServiceIntegrationTests {

	@Container
	static final ComposeContainer COMPOSE = new ComposeContainer(new File("compose.yaml"))
			.withExposedService("dynamodb", 8000)
			.withExposedService("kafka", 9092)
			.withExposedService("mongodb", 27017)
			.withExposedService("cart-service", 8002)
			.withLocalCompose(true);

	@DynamicPropertySource
	static void register(DynamicPropertyRegistry registry){
		registry.add("DYNAMODB_URL", () -> "http://".concat(hostAndPort("dynamodb")));
		registry.add("kafka.bootstrap-servers", () -> hostAndPort("kafka"));
		registry.add("MONGODB_URL", () -> "mongodb://".concat(hostAndPort("mongodb")));
	}

	@LocalServerPort
	int orderService;
	@Autowired
	MongoClient mongoClient;
	int cartService;

	@Test
	void contextLoads() {
		var accessToken = accessTokenInHeader("bob@bmail.com");

		var productId = insertProductIntoMongo("laptop", "X-Laptop", 15, 230_99);

		given().port(cartService).header(accessToken).contentType(ContentType.JSON).body("""
				{ "productId": "%s", "quantity": 5 }
				""".formatted(productId))
				.when().patch("/cart")
				.then().statusCode(200);

		var orderDetials = given().port(orderService).header(accessToken)
				.when().post("/orders")
				.then().statusCode(200)
				.extract().body().as(OrderDetails.class);

		var orders = given().port(orderService).header(accessToken)
				.when().get("/orders")
				.then().statusCode(200)
				.extract().body().jsonPath().getList("$", UserOrderView.class);

		assertThat(orders).singleElement().satisfies(view -> {
			assertThat(view.orderId()).isEqualTo(orderDetials.orderId());
			assertThat(view.totalPrice()).isEqualTo(orderDetials.items().stream().mapToInt(OrderItem::price).sum());
			assertThat(view.placedAt()).isEqualTo(orderDetials.placedAt());
		});
	}

	private String insertProductIntoMongo(String category, String name, int quantity, int price){
		return mongoClient.getDatabase("sellersphere").getCollection("products")
				.insertOne(new Document()
						.append("category", category)
						.append("productName", name)
						.append("quantity", quantity)
						.append("price", price)
						.append("version", 1))
				.getInsertedId().asObjectId().getValue().toHexString();
	}

	private Header accessTokenInHeader(String email){
		var accessToken = JWT.create()
				.withIssuer(jwtIssuer)
				.withExpiresAt(Instant.now().plusSeconds(600))
				.withClaim("SessionId", "randomId")
				.withClaim("Scope", "user")
				.withSubject("USER#".concat(email))
				.sign(Algorithm.HMAC256(jwtSecret));
		return new Header("Authorization", "Bearer ".concat(accessToken));
	}

	@Value("${security.jwt.secret}")
	String jwtSecret;
	String jwtIssuer = "https://seller-sphere.com";

	@BeforeEach
	void setPorts(){
		cartService = COMPOSE.getServicePort("cart-service", 8002);
	}

	private static String hostAndPort(String service){
		var containerState = COMPOSE.getContainerByServiceName(service).orElseThrow();
		return containerState.getHost() + ":" + containerState.getFirstMappedPort();
	}

}
