package com.example.seminaranalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final InMemoryVectorStore vectorStore;
    private final EmbeddingService    embeddingService;

    @Value("${app.rag.top-k}")
    private int topK;

    /**
     * Embeds the fingerprint and retrieves the top-K most
     * structurally similar papers from the knowledge base.
     *
     * @param fingerprint  Structural description of the new paper (from LLM #1)
     * @return             Top-K most similar VectorEntries
     */
    public List<InMemoryVectorStore.VectorEntry> findSimilar(String fingerprint) {
        List<Double> queryVector = embeddingService.embedQuery(fingerprint);
        List<InMemoryVectorStore.VectorEntry> results = vectorStore.findTopK(queryVector, topK);

        log.info("Retrieved {} similar papers: {}", results.size(),
                results.stream().map(e -> e.metadata().get("id")).toList());

        return results;
    }

    /**
     * Formats the retrieved papers into a few-shot block for the second LLM call.
     *
     * @param entries  Retrieved similar papers
     * @return         Formatted string to inject into the grader prompt
     */
    public String formatAsExamples(List<InMemoryVectorStore.VectorEntry> entries) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < entries.size(); i++) {
            Map<String, Object> m = entries.get(i).metadata();

            sb.append("--- EXAMPLE ").append(i + 1).append(" ---\n");
            sb.append("Structural profile: ").append(m.get("fingerprint")).append("\n\n");
            sb.append("Introduction (").append(m.get("intro_score")).append("/10): ")
                    .append(m.get("intro_annotation")).append("\n\n");
            sb.append("Body (").append(m.get("body_score")).append("/35): ")
                    .append(m.get("body_annotation")).append("\n\n");
            sb.append("Conclusion (").append(m.get("conclusion_score")).append("/25): ")
                    .append(m.get("conclusion_annotation")).append("\n\n");
            sb.append("Citation penalty applied: ").append(m.get("citation_penalty")).append("\n");
            sb.append("Word count factor: ").append(m.get("word_count_factor")).append("\n");
            sb.append("Source factor: ").append(m.get("source_factor")).append("\n");
            sb.append("Structural issues: ").append(m.get("structural_issues")).append("\n");
            sb.append("Final score: ").append(m.get("original_total")).append("\n");
            sb.append("Professor comment: ").append(m.get("original_comment")).append("\n\n");
        }

        return sb.toString();
    }
}
