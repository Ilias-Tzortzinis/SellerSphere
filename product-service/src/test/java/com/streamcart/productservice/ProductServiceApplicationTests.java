package com.streamcart.productservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.streamcart.productservice.model.Laptop;
import com.streamcart.productservice.model.Product;
import com.streamcart.productservice.model.ProductView;
import io.restassured.RestAssured;
import org.bson.Document;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.print.Doc;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;


@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

	@Container
	static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0.0");
	
	@Autowired
	MongoClient mongoClient;
	MongoCollection<Document> products;
	@LocalServerPort
	int springBootPort;

	@Test
	@DisplayName("Product searching is paginated")
	void productSearchingIsPaginated() {
		int pageSize = 15;
		var totalProducts = 100;
		generateLaptops().stream().limit(totalProducts).forEach(this::insertIntoProducts);

		var result = when().get("/products/search/laptop")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", ProductView.class);
		assertThat(result).hasSize(pageSize);

		int productsSeen = pageSize;
		do {
			var lastId = result.getLast().productId();
		 	result = given().queryParam("lastId", lastId)
					.when().get("/products/search/laptop")
					.then().statusCode(200)
					.extract().jsonPath().getList("$", ProductView.class);
			 productsSeen += result.size();
		} while (result.size() == pageSize);
		assertThat(productsSeen).isEqualTo(totalProducts);
	}

	@Test
	@DisplayName("Query products in price range")
	void queryProductsInPriceRange() {
		var laptops = generateLaptops().stream().limit(15).peek(this::insertIntoProducts).toList();
		int min_price = 400, max_price = 900;
		var count = laptops.stream().filter(l -> l.price() >= min_price && l.price() <= max_price).count();

		var result = given().queryParams("min_price", min_price, "max_price", max_price)
				.when().get("/products/search/laptop")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", ProductView.class);

		assertThat(result.size()).isEqualTo(count);
	}

	@Test
	@DisplayName("Passing an invalid productId into getProductById results in 400")
	void passingAnInvalidProductIdIntoGetProductByIdResultsIn400() {
		when().get("/products/{productId}", "badId")
				.then().statusCode(400);
	}

	private void insertIntoProducts(Laptop laptop) {
		products.insertOne(new Document()
				.append("category", laptop.category().name())
				.append("quantity", laptop.quantity())
				.append("price", laptop.price())
				.append("ram", laptop.ram()));
	}

	@BeforeEach
	void refresh(){
		products = mongoClient.getDatabase("streamcart").getCollection("products");
		products.deleteMany(Filters.empty());
		RestAssured.port = springBootPort;
	}

	private InstancioApi<Laptop> generateLaptops(){
		return Instancio.of(Laptop.class)
				.withNullable(field("productId"))
				.generate(field("brand"), gen -> gen.oneOf("Samsung x500", "Lenovo L40", "Apple M20", "Samsung X900", "Lenovo L50", "Apple M30"))
				.generate(field("quantity"), gen -> gen.ints().min(0).max(300))
				.generate(field("price"), gen -> gen.doubles().min(230D).max(1800D))
				.generate(field("ram"), gen -> gen.oneOf(4, 8, 12, 16, 32));
	}

	@TestConfiguration
	static class Config {
		@Bean
		@Primary
		public MongoClient mongoTestClient(){
			System.out.println();
			return MongoClients.create(MONGO.getConnectionString());
		}
	}

}
