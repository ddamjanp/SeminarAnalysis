package com.example.seminaranalysis.service;


import com.example.seminaranalysis.dto.GradeResponse;
import com.example.seminaranalysis.dto.SectionGrade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class GraderService {

    private final ChatService  chatService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a strict but fair grader for a Computer Ethics university course.
            You grade based on structure and ethical depth — not topic knowledge.
            You always return valid JSON and nothing else.
            No markdown, no explanation, no code fences — just the raw JSON object.
            """;

    private static final String USER_PROMPT = """
            Grade this Computer Ethics seminar paper.

            ══════════════════════════════════════════
            GRADING SYSTEM (70 points total)
            ══════════════════════════════════════════

            INTRODUCTION — 10 points
            • Ideal length: 150-200 words
            • Must briefly explain the topic
            • Must announce what ethical aspects will be explored
            • Deduct for: too long (>250 words), too short (<100 words), no ethical framing

            BODY — 35 points
            • PRIMARY: focus on ethical challenges, risks, dilemmas — NOT technical descriptions
            • Arguments supported by concrete real-world examples
            • Sources cited INLINE with [n] or (Author, Year) throughout the text
            • Heavily penalize: technical-only papers, descriptive summaries, LLM-generated text

            CONCLUSION — 25 points
            • Synthesizes the ethical insights from the body
            • Includes the STUDENT'S OWN stance and concrete recommendations
            • Penalize: generic statements, missing personal stance, just restating intro

            ══════════════════════════════════════════
            MULTIPLIERS (you do NOT compute these — just provide the flags)
            ══════════════════════════════════════════
            • Citation penalty: if sources NOT cited inline → set citation_penalty: true
            • Word count and source count factors are computed by the system automatically

            ══════════════════════════════════════════
            SIMILAR PREVIOUSLY GRADED PAPERS
            ══════════════════════════════════════════
            {examples}

            ══════════════════════════════════════════
            NEW PAPER TO GRADE
            ══════════════════════════════════════════
            Structural profile: {fingerprint}
            Word count: {wordCount}
            Inline citations detected: {inlineCitations}

            {paperText}

            ══════════════════════════════════════════
            Respond with ONLY this JSON — no other text:
            ══════════════════════════════════════════
            {
              "intro_score": <integer 0-10>,
              "intro_feedback": "<specific feedback on the introduction>",
              "body_score": <integer 0-35>,
              "body_feedback": "<specific feedback on the body>",
              "conclusion_score": <integer 0-25>,
              "conclusion_feedback": "<specific feedback on the conclusion>",
              "citation_penalty": <true if sources not cited inline, false otherwise>,
              "source_count": <your best estimate of number of sources>,
              "structural_issues": ["issue1", "issue2"],
              "overall_feedback": "<2-3 sentence summary of strengths and what to improve>"
            }
            """;

    /**
     * Grades the paper using few-shot examples from RAG.
     * The LLM returns section scores and flags as JSON.
     * Multipliers are applied deterministically in Java.
     *
     * @param paperText      Full text of the paper
     * @param fingerprint    Structural description from LLM #1
     * @param examples       Formatted few-shot examples from RagService
     * @param wordCount      Actual word count from DocumentTextExtractor
     * @param inlineCitations Number of inline citations detected
     * @return               Complete GradeResponse with scores and feedback
     */
    public GradeResponse grade(String paperText, String fingerprint,
                               String examples, int wordCount, int inlineCitations) {
        String prompt = USER_PROMPT
                .replace("{examples}",       examples)
                .replace("{fingerprint}",    fingerprint)
                .replace("{wordCount}",      String.valueOf(wordCount))
                .replace("{inlineCitations}",String.valueOf(inlineCitations))
                .replace("{paperText}",      paperText.length() > 8000
                        ? paperText.substring(0, 8000)
                        : paperText);

        String rawJson = chatService.chat(SYSTEM_PROMPT, prompt);
        log.debug("Grader raw response: {}", rawJson);

        return parseAndCompute(rawJson, wordCount);
    }

    private GradeResponse parseAndCompute(String rawJson, int wordCount) {
        try {

            String json = rawJson.replaceAll("(?s)```(?:json)?\\s*", "").trim();
            JsonNode node = objectMapper.readTree(json);

            int     introScore      = node.get("intro_score").asInt();
            int     bodyScore       = node.get("body_score").asInt();
            int     conclusionScore = node.get("conclusion_score").asInt();
            boolean citationPenalty = node.get("citation_penalty").asBoolean();
            int     sourceCount     = node.get("source_count").asInt();


            double citFactor  = citationPenalty ? 0.7 : 1.0;
            double wcFactor   = computeWordCountFactor(wordCount);
            double srcFactor  = computeSourceFactor(sourceCount);

            double rawScore   = introScore + bodyScore + conclusionScore;
            double finalScore = Math.round(rawScore * citFactor * wcFactor * srcFactor * 100) / 100.0;

            List<String> issues = new ArrayList<>();
            node.get("structural_issues").forEach(n -> issues.add(n.asText()));

            return GradeResponse.builder()
                    .intro(SectionGrade.builder()
                            .score(introScore).maxScore(10)
                            .feedback(node.get("intro_feedback").asText())
                            .build())
                    .body(SectionGrade.builder()
                            .score(bodyScore).maxScore(35)
                            .feedback(node.get("body_feedback").asText())
                            .build())
                    .conclusion(SectionGrade.builder()
                            .score(conclusionScore).maxScore(25)
                            .feedback(node.get("conclusion_feedback").asText())
                            .build())
                    .citationPenalty(citationPenalty)
                    .wordCount(wordCount)
                    .wordCountFactor(Math.round(wcFactor * 1000.0) / 1000.0)
                    .sourceCount(sourceCount)
                    .sourceFactor(Math.round(srcFactor * 1000.0) / 1000.0)
                    .rawScore(rawScore)
                    .finalScore(finalScore)
                    .overallFeedback(node.get("overall_feedback").asText())
                    .structuralIssues(issues)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse grader response: {}", rawJson, e);
            throw new RuntimeException("Grading failed — could not parse LLM response.", e);
        }
    }


    private double computeWordCountFactor(int n) {
        if (n < 500 || n > 2000) return 0.5;
        if (n < 900)             return (double) n / 900.0;
        if (n > 1100)            return 1100.0 / (double) n;
        return 1.0;
    }

    private double computeSourceFactor(int n) {
        if (n >= 10) return 1.0;
        return Math.max(0.5, 1.0 - (10 - n) * 0.05);
    }
}