package com.example.seminaranalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseLoader implements ApplicationRunner {

    private final InMemoryVectorStore vectorStore;
    private final EmbeddingService    embeddingService;
    private final ObjectMapper        objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Loading knowledge base — making 31 embedding API calls, please wait...");

        ClassPathResource resource = new ClassPathResource("data/knowledge_base.json");

        try (InputStream is = resource.getInputStream()) {
            JsonNode entries = objectMapper.readTree(is);
            int count = 0;

            for (JsonNode entry : entries) {
                String id          = entry.get("id").asText();
                String fingerprint = entry.get("fingerprint").asText();


                Map<String, Object> metadata = new HashMap<>();
                metadata.put("id",                    id);
                metadata.put("title",                 entry.get("title").asText());
                metadata.put("fingerprint",           fingerprint);
                metadata.put("intro_score",           entry.get("intro").get("score").asInt());
                metadata.put("intro_annotation",      entry.get("intro").get("annotation").asText());
                metadata.put("body_score",            entry.get("body").get("score").asInt());
                metadata.put("body_annotation",       entry.get("body").get("annotation").asText());
                metadata.put("conclusion_score",      entry.get("conclusion").get("score").asInt());
                metadata.put("conclusion_annotation", entry.get("conclusion").get("annotation").asText());
                metadata.put("citation_penalty",      entry.get("citation_penalty").asBoolean());
                metadata.put("word_count_factor",     entry.get("word_count_factor").asDouble());
                metadata.put("source_factor",         entry.get("source_factor").asDouble());
                metadata.put("structural_issues",     entry.get("structural_issues").toString());
                metadata.put("original_total",        entry.get("original_grade").get("total").asDouble());
                metadata.put("original_comment",      entry.get("original_grade").get("comment").asText());


                vectorStore.add(id, embeddingService.embedDocument(fingerprint), metadata);

                log.info("  Indexed [{}/31] {}", ++count, id);
            }

            log.info("Knowledge base ready — {} papers indexed.", count);
        }
    }
}