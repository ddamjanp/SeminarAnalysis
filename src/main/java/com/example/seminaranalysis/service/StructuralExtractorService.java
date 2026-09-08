package com.example.seminaranalysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class StructuralExtractorService {

    private final ChatService chatService;

    private static final String SYSTEM_PROMPT = """
            You are a structural analyst for academic papers.
            Your job is to describe HOW a paper is written — not what topic it covers.
            Focus exclusively on structural and stylistic features.
            Never mention the subject area or topic content.
            """;

    private static final String USER_PROMPT = """
            Analyze the structure of this Computer Ethics seminar paper.
            Write 2-3 sentences describing ONLY these structural features:

            1. Introduction: estimated word count — is it within 150-200 words?
               Does it announce an ethical focus or just describe the topic?
            2. Body: what fraction is ethical analysis vs technical description?
               Are concrete real-world examples present?
            3. Citations: are sources cited inline with [n] or (Author, Year) throughout the text?
               Or are they only listed at the end with no inline references?
            4. Conclusion: does it synthesize the body's arguments?
               Does the student express their OWN stance and concrete recommendations?
            5. Rough estimate of total word count and number of sources.

            PAPER:
            {text}
            """;

    /**
     * Produces a structural fingerprint for the given paper text.
     * Trims to 8000 characters to avoid context window issues.
     *
     * @param paperText  Full extracted text of the paper
     * @return           2-3 sentence structural description
     */
    public String extractFingerprint(String paperText) {
        String trimmed = paperText.length() > 8000
                ? paperText.substring(0, 8000)
                : paperText;

        return chatService.chat(
                SYSTEM_PROMPT,
                USER_PROMPT.replace("{text}", trimmed)
        );
    }
}