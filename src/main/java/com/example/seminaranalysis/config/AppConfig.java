package com.example.seminaranalysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /** RestClient for OpenRouter — used by ChatService for LLM calls */
    @Bean
    public RestClient openRouterRestClient(@Value("${openrouter.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /** RestClient for Cohere — used by EmbeddingService only */
    @Bean
    public RestClient cohereRestClient(@Value("${cohere.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl("https://api.cohere.com/v2")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}