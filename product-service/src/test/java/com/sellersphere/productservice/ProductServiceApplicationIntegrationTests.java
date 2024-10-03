package com.sellersphere.productservice;

import com.mongodb.client.MongoClient;
import com.sellersphere.productservice.data.Laptop;
import com.sellersphere.productservice.data.ProductOverview;
import io.restassured.RestAssured;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static com.sellersphere.productservice.data.ProductCategory.LAPTOP;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationIntegrationTests {

	@Container
	static final MongoDBContainer MONGO_DB = new MongoDBContainer(DockerImageName.parse("mongo:8.0.0"));

	@DynamicPropertySource
	static void registerMongo(DynamicPropertyRegistry registry){
		registry.add("MONGO_URL", MONGO_DB::getConnectionString);
	}

	@LocalServerPort
	int productService;
	@Autowired
	MongoClient mongoClient;

	@Test
	@DisplayName("Fetch products in price range")
	void fetchProductsInPriceRange() {
		int minPrice = 500, maxPrice = 800;
		int expectedCount = (int) Instancio.of(Laptop.class)
				.ignore(field(Laptop::productId))
				.generate(field(Laptop::price), gen -> gen.ints().range(230, 1400).as(Integer::doubleValue))
				.stream()
				.limit(15)
				.peek(this::saveIntoMongoDB)
				.filter(l -> l.price() >= minPrice && l.price() <= maxPrice)
				.count();

		assertThat(given().port(productService).queryParams("minPrice", minPrice, "maxPrice", maxPrice)
				.when().get("/products/search/laptop")
				.then().statusCode(200)
				.extract().jsonPath().getList("$", ProductOverview.class))
				.hasSize(expectedCount);
	}

	@Test
	@DisplayName("Find product by valid id")
	void findProductByValidId() {
		var productId = new ObjectId().toHexString();
		var laptop = Instancio.of(Laptop.class)
				.set(field(Laptop::productId), productId)
				.create();
		saveIntoMongoDB(laptop);

		assertThat(given().port(productService)
				.when().get("/products/{productId}", productId)
				.then().statusCode(200)
				.extract().body().as(Laptop.class))
				.isEqualTo(laptop);
	}
	
	private void saveIntoMongoDB(Laptop laptop){
		var document = new Document()
				.append("category", LAPTOP.name())
				.append("quantity", laptop.quantity())
				.append("price", laptop.price())
				.append("ram", laptop.ram());
        if (laptop.productId() != null) {
            document.append("_id", new ObjectId(laptop.productId()));
        }
		mongoClient.getDatabase("sellersphere").getCollection("products").insertOne(document);
	}

}
