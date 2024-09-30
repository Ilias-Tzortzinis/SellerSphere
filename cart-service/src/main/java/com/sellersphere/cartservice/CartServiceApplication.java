package com.sellersphere.cartservice;

import com.sellersphere.authorization.JwtAuthorizationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;

@SpringBootApplication(scanBasePackages = "com.sellersphere", exclude = {SecurityAutoConfiguration.class})
public class CartServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartServiceApplication.class, args);
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

}
