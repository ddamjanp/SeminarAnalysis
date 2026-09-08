package com.example.seminaranalysis.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class GradeResponse {

    // ── Section scores ──────────────────────────────────────────────────────
    private SectionGrade intro;        // out of 10
    private SectionGrade body;         // out of 35
    private SectionGrade conclusion;   // out of 25

    // ── Multipliers ─────────────────────────────────────────────────────────
    private boolean citationPenalty;   // true = sources not cited inline → ×0.7
    private int     wordCount;         // actual word count of the paper
    private double  wordCountFactor;   // n/900 or 1100/n
    private int     sourceCount;       // estimated number of sources
    private double  sourceFactor;      // 1 - (10 - n) × 0.05, min 0.5

    // ── Final score ─────────────────────────────────────────────────────────
    private double rawScore;           // intro + body + conclusion (max 70)
    private double finalScore;         // rawScore × all multipliers

    // ── Feedback ────────────────────────────────────────────────────────────
    private String       overallFeedback;
    private List<String> structuralIssues;
}