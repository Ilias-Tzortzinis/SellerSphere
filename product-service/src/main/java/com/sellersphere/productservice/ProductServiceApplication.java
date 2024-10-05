package com.sellersphere.productservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@SpringBootApplication(scanBasePackages = "com.sellersphere", exclude = {SecurityAutoConfiguration.class})
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(CorsFilter corsFilter){
		return new SecurityFilterChain() {
			@Override
			public boolean matches(HttpServletRequest request) {
				return true;
			}

			@Override
			public List<Filter> getFilters() {
				return List.of(corsFilter);
			}
		};
	}

	@Bean
	public CorsFilter corsFilter(){
		var cors = new CorsConfiguration();
		cors.addAllowedOrigin("*");
		cors.setAllowedMethods(List.of("GET", "POST"));
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return new CorsFilter(source);
	}

	@Bean
	public MongoClient mongoClient(@Value("${MONGO_URL}") String url){
		return MongoClients.create(url);
	}
}
