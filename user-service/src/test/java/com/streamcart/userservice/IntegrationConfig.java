package com.streamcart.userservice;

import com.streamcart.userservice.security.JsonWebTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

import static com.streamcart.userservice.UserServiceApplicationIntegrationTests.COMPOSE;

@TestConfiguration
class IntegrationConfig {
    @Bean
    @Primary
    public DynamoDbClient dynamoDb() {
        var host = COMPOSE.getServiceHost("dynamodb-local", 8000);
        var port = COMPOSE.getServicePort("dynamodb-local", 8000);
        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://" + host + ":" + port))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.builder()
                        .secretAccessKey("secret").accessKeyId("access").build()))
                .region(Region.US_WEST_2)
                .build();
    }

    @Bean
    @Primary
    public MailSender mailSender() {
        var host = COMPOSE.getServiceHost("mailhog", 1025);
        var port = COMPOSE.getServicePort("mailhog", 1025);
        var sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setProtocol("smtp");
        return sender;
    }

    @Bean
    @Primary
    public JsonWebTokenService webTokenService(DynamoDbClient dynamoDb) {
        return new JsonWebTokenService(dynamoDb, "secret", "PT10M", "P1D");
    }

    @Bean
    @Primary
    public UserService userService(DynamoDbClient dynamoDbClient){
        return new UserService(dynamoDbClient, () -> UserServiceApplicationIntegrationTests.CLOCK.get());
    }
}
