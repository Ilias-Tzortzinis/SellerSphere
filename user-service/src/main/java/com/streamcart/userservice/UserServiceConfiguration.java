package com.streamcart.userservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
public class UserServiceConfiguration {

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${dynamodb.uri}") String uri,
                                         @Value("${dynamodb.region}") String region,
                                         @Value("${dynamodb.access-key-id}") String accessKeyId,
                                         @Value("${dynamodb.secret-access-key}") String secretAccessKey){
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(uri))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                        .accessKeyId(accessKeyId).secretAccessKey(secretAccessKey).build()))
                .build();
    }

}
