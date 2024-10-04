package com.sellersphere.integration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.commons.logging.impl.Log4JLogger;
import org.bson.Document;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.time.*;
import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Testcontainers
public final class UserPlaceOrderIntegrationTest {

    @Container
    static final ComposeContainer COMPOSE = new ComposeContainer(new File("integration-compose.yaml"))
            .withExposedService("user-service", 8080)
            .withExposedService("cart-service", 8002)
            .withExposedService("product-service", 8080)
            .withExposedService("order-service", 8080)
            .withExposedService("mailhog", 8025)
            .withExposedService("mongodb", 27017)
            .withLocalCompose(true);

    int userService, cartService, productService, orderService, mailhog;


    @Test
    @DisplayName("Integration Test")
    void integrationTest() {
        var userEmail = "bob@bmail.com";
        var userCredentials = """
                { "email":  "%s", "password":  "bobthebest"}
                """.formatted(userEmail);
        given().port(userService).contentType(ContentType.JSON).body(userCredentials)
                .when().post("/users/signup")
                .then().statusCode(201);

        var mail = given().port(mailhog)
                .when().get("/api/v1/messages")
                .then().statusCode(200)
                .extract().jsonPath().param("mail", userEmail)
                .getString("find(msg -> msg.Content.Headers.To[0] == mail).Content.Body");
        var offset = mail.lastIndexOf("VerificationCode: ") + "VerificationCode: ".length();
        var verificationCode = mail.substring(offset, offset + 6);

        given().port(userService).contentType(ContentType.JSON).body("""
                          { "email": "%s", "verificationCode": "%s" }
                          """.formatted(userEmail, verificationCode))
                .when().patch("/users/verify/signup")
                .then().statusCode(200);

        var accessToken = given().port(userService).contentType(ContentType.JSON).body(userCredentials)
                .when().post("/users/login")
                .then().statusCode(200)
                .extract().header("X-ACCESS-TOKEN");
        var authorization = new Header("Authorization", "Bearer ".concat(accessToken));

        var products = given().port(productService)
                .queryParams("minPrice", 500_00, "maxPrice", 900_00)
                .when().get("/products/search/laptop")
                .then().statusCode(200)
                .extract().jsonPath().getList("$", LaptopOverview.class);

        for (LaptopOverview product : products) {
            given().port(cartService).header(authorization)
                    .contentType(ContentType.JSON)
                    .body(new CartItem(product.productId(), Math.min(15, product.quantity())))
                    .when().patch("/cart")
                    .then().statusCode(200);
        }

        var cart = given().port(cartService).header(authorization)
                .when().get("/cart")
                .then().statusCode(200)
                .extract().jsonPath().getList("$", CartItem.class);
        assertEquals(products.size(), cart.size());

        var orderDetails = given().port(orderService).header(authorization)
                .when().post("/orders")
                .then().statusCode(200)
                .extract().body().as(OrderDetails.class);

        assertThat(given().port(orderService).header(authorization)
                .when().get("/orders")
                .then().statusCode(200)
                .extract().jsonPath().getList("$", OrderView.class))
                .singleElement().satisfies(view -> assertThat(view.orderId()).isEqualTo(orderDetails.orderId()));
    }

    record OrderView(String orderId, String status, long placedAt, int totalPrice){}

    record OrderDetails(String orderId, String status, long placedAt, List<OrderItem> items){}

    record OrderItem(String productId, String productName, int quantity, int price){}

    @BeforeEach
    void setUpTestData(){
        userService = COMPOSE.getServicePort("user-service", 8080);
        cartService = COMPOSE.getServicePort("cart-service", 8002);
        productService = COMPOSE.getServicePort("product-service", 8080);
        orderService = COMPOSE.getServicePort("order-service", 8080);
        mailhog = COMPOSE.getServicePort("mailhog", 8025);

        ContainerState mongodb = COMPOSE.getContainerByServiceName("mongodb").orElseThrow();
        var connectionString = "mongodb://" + mongodb.getHost() + ":" + mongodb.getFirstMappedPort();
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            var collection = mongoClient.getDatabase("sellersphere").getCollection("products");
            var laptops = Instancio.of(Laptop.class)
                    .ignore(field(Laptop::productId))
                    .set(field(Laptop::category), "LAPTOP")
                    .generate(field(Laptop::quantity), gen -> gen.ints().range(0, 500))
                    .generate(field(Laptop::price), gen -> gen.ints().range(230_00, 1900_00))
                    .generate(field(Laptop::productName), gen -> gen.text().pattern("Laptop #C#a#a#a"))
                    .generate(field(Laptop::images), gen -> gen.collection().minSize(1).maxSize(6).with("http://mock-url/image1", "http://mock-url/image2"))
                    .stream()
                    .limit(50)
                    .map(Laptop::document)
                    .toList();
            collection.insertMany(laptops);
        }

    }

    record CartItem(String productId, int quantity){}

    record LaptopOverview(String productId, int quantity, int price){ }

    record Laptop(String productId, String category, int quantity, int price, String productName,
                  List<String> images, String description){

        static final RecordComponent[] FIELDS = Laptop.class.getRecordComponents();

        Document document(){
            var document = new Document();
            for (var field : FIELDS) {
                if (field.getName().equals("productId")) continue;
                try {
                    document.append(field.getName(), field.getAccessor().invoke(this));
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
            document.append("version", 0);
            return document;
        }
    }
}
