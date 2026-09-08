package com.example.seminaranalysis.service;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.Loader;
import java.io.IOException;
import java.util.regex.Pattern;


@Service
public class DocumentTextExtractor {

    /**
     * Extracts plain text from the uploaded file.
     *
     * @param file  The uploaded file (PDF or .txt)
     * @return      Plain text content
     */
    public String extract(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file);
        }

        // Default: treat as plain text
        return new String(file.getBytes());
    }



    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * Counts words by splitting on whitespace.
     * Used to compute the word count scaling factor.
     */
    public int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * Counts inline source citations in the text.
     * Matches two common formats:
     *   [1], [2], [12]          → bracket style
     *   (Author, 2024)          → author-year style
     * If neither format is found, the citation penalty will be applied.
     */
    public int countInlineCitations(String text) {
        long bracketStyle    = Pattern.compile("\\[\\d+]")
                .matcher(text).results().count();

        long authorYearStyle = Pattern.compile("\\([A-Z][a-z]+.*?\\d{4}\\)")
                .matcher(text).results().count();

        return (int) Math.max(bracketStyle, authorYearStyle);
    }
}