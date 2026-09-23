package com.pedidos360.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient catalogoWebClient(
            @Value("${service.catalogo.url:http://localhost:8082}") String catalogoUrl) {
        return WebClient.builder()
                .baseUrl(catalogoUrl)
                .build();
    }
}