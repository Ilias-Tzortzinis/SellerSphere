package com.streamcart.productservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class ProductServiceConfiguration {

    @Bean
    @Profile("!test")
    public MongoClient mongoClient(@Value("${mongodb.url}") String url){
        return MongoClients.create(url);
    }

    @Bean
    public SecurityFilterChain securityFilterChain() throws Exception {
        return new SecurityFilterChain() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return true;
            }

            @Override
            public List<Filter> getFilters() {
                return List.of();
            }
        };
    }
}
