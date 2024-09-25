package com.streamcart.cartservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@TestConfiguration
class IntegrationTestsConfiguration {
    @Bean
    @Primary
    public DynamoDbClient dynamoDbTestClient() {
        var host = CartServiceApplicationIntegrationTests.COMPOSE.getServiceHost("dynamodb", 8000);
        var port = CartServiceApplicationIntegrationTests.COMPOSE.getServicePort("dynamodb", 8000);
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://" + host + ":" + port))
                .region(Region.US_WEST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                        .accessKeyId("access").secretAccessKey("secret").build()))
                .build();
    }
}
