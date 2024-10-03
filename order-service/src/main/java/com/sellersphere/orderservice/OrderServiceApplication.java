package com.sellersphere.orderservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.sellersphere.authorization.JwtAuthorizationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.security.web.SecurityFilterChain;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@SpringBootApplication(scanBasePackages = "com.sellersphere", exclude = {SecurityAutoConfiguration.class})
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(JwtAuthorizationFilter authorizationFilter){
		return new SecurityFilterChain() {
			@Override
			public boolean matches(HttpServletRequest request) {
				return true;
			}

			@Override
			public List<Filter> getFilters() {
				return List.of(authorizationFilter);
			}
		};
	}

	@Bean
	public DynamoDbClient dynamoDbClient(@Value("${dynamodb.url}") String url,
										 @Value("${aws.region}") String region,
										 @Value("${aws.access-key-id}") String accessKeyId,
										 @Value("${aws.secret-access-key}") String secretAccessKey){
		return DynamoDbClient.builder()
				.endpointOverride(URI.create(url))
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
				.build();
	}

	@Bean
	public MongoClient mongoClient(@Value("${mongodb.url}") String url){
		return MongoClients.create(url);
	}

	@Bean
	public ProducerFactory<String, String> producerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers){
		var configs = Map.<String, Object>of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
		);
		var serializer = new StringSerializer();
		return new DefaultKafkaProducerFactory<>(configs, serializer, serializer);
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory){
		return new KafkaTemplate<>(producerFactory);
	}
}
