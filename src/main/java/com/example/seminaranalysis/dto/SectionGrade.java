package com.example.seminaranalysis.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SectionGrade {
    private int    score;     // Points awarded
    private int    maxScore;  // Maximum possible (10, 35, or 25)
    private String feedback;  // Specific feedback for this section
}