package com.example.seminaranalysis.controller;

import com.example.seminaranalysis.dto.GradeResponse;
import com.example.seminaranalysis.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GradeController {

    private final DocumentTextExtractor      textExtractor;
    private final StructuralExtractorService structuralExtractor;
    private final RagService                 ragService;
    private final GraderService              graderService;

    /**
     * POST /api/grade
     * Accepts a seminar paper as a multipart file (PDF or .txt).
     * Returns a structured grade with section-level feedback and multipliers.
     *
     * @param file  The uploaded seminar paper
     * @return      GradeResponse with scores, multipliers, and feedback
     */
    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> grade(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received file: {}, size: {} bytes",
                    file.getOriginalFilename(), file.getSize());


            String text         = textExtractor.extract(file);
            int    wordCount    = textExtractor.countWords(text);
            int    inlineCites  = textExtractor.countInlineCitations(text);

            log.info("Extracted {} words, {} inline citations", wordCount, inlineCites);

            // structural fingerprint / first call
            String fingerprint = structuralExtractor.extractFingerprint(text);
            log.info("Fingerprint: {}", fingerprint);


            var    similar  = ragService.findSimilar(fingerprint);
            String examples = ragService.formatAsExamples(similar);

            // Second call to LLM, grade with other papers
            GradeResponse response = graderService.grade(
                    text, fingerprint, examples, wordCount, inlineCites
            );

            log.info("Grading complete. Final score: {}/70", response.getFinalScore());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Grading failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Seminar Grader is running.");
    }
}