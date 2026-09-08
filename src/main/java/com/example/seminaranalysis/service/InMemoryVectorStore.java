package com.example.seminaranalysis.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@Component
public class InMemoryVectorStore {

    private final List<VectorEntry> entries = new ArrayList<>();

    /**
     * Stores a vector alongside its metadata.
     *
     * @param id        Unique identifier (e.g. "hybrid_00")
     * @param vector    The embedded fingerprint (1536 numbers from OpenAI)
     * @param metadata  Everything else — section scores, annotations, issues
     */
    public synchronized void add(String id, List<Double> vector, Map<String, Object> metadata) {
        entries.add(new VectorEntry(id, vector, metadata));
    }

    /**
     * Finds the top-K most similar entries to the given query vector.
     * Similarity is measured using cosine similarity — closer to 1.0 = more similar.
     *
     * @param queryVector  The embedded fingerprint of the new paper
     * @param k            How many similar papers to return (configured as app.rag.top-k)
     * @return             The k most similar VectorEntries, sorted best-first
     */
    public List<VectorEntry> findTopK(List<Double> queryVector, int k) {
        return entries.stream()
                .sorted(Comparator.comparingDouble(
                        entry -> -cosineSimilarity(entry.vector(), queryVector)
                ))
                .limit(k)
                .toList();
    }

    /**
     * Measures the similarity between two vectors.
     * Returns a value between -1.0 and 1.0 — higher means more similar.
     * We negate it in findTopK to sort descending (most similar first).
     */
    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot   = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.size(); i++) {
            dot   += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Represents one entry in the vector store.
     *
     * @param id        Paper identifier
     * @param vector    Embedded fingerprint
     * @param metadata  Section scores, annotations, structural issues, original grade
     */
    public record VectorEntry(String id, List<Double> vector, Map<String, Object> metadata) {}
}