package com.example.seminaranalysis.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;


@Service
public class EmbeddingService {

    private final RestClient cohereRestClient;
    private final String model;

    public EmbeddingService(
            @Qualifier("cohereRestClient") RestClient cohereRestClient,
            @Value("${cohere.embedding.model}") String model
    ) {
        this.cohereRestClient = cohereRestClient;
        this.model = model;
    }


    public List<Double> embedDocument(String text) {
        return embed(text, "search_document");
    }


    public List<Double> embedQuery(String text) {
        return embed(text, "search_query");
    }

    private List<Double> embed(String text, String inputType) {
        var requestBody = Map.of(
                "texts",           List.of(text),
                "model",           model,
                "input_type",      inputType,
                "embedding_types", List.of("float")
        );

        EmbeddingResponse response = cohereRestClient.post()
                .uri("/embed")
                .body(requestBody)
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.embeddings().floatEmbeddings().get(0);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingResponse(EmbeddingData embeddings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingData(@JsonProperty("float") List<List<Double>> floatEmbeddings) {}
}