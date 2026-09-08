package com.example.seminaranalysis.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;


@Service
public class ChatService {

    private final RestClient openRouterRestClient;

    @Value("${openai.chat.model}")
    private String model;

    public ChatService(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Value("${openai.chat.model}") String model
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.model = model;
    }

    /**
     * @param systemPrompt  Instructions that define how the model should behave
     * @param userPrompt    The actual content/question to process
     * @return              The model's response as a plain String
     */
    public String chat(String systemPrompt, String userPrompt) {
        var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
        );

        var requestBody = Map.of(
                "model",    model,
                "messages", messages
        );

        ChatResponse response = openRouterRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(ChatResponse.class);

        return response.choices().get(0).message().content();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {}
}