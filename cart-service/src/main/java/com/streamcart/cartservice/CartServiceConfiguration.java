package com.streamcart.cartservice;

import com.streamcart.cartservice.security.JwtAuthorizationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;

@Configuration
public class CartServiceConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(JwtAuthorizationFilter filter){
        return new SecurityFilterChain() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return true;
            }

            @Override
            public List<Filter> getFilters() {
                return List.of(filter);
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
