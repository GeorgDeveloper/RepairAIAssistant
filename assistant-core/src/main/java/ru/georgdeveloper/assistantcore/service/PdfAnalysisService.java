package ru.georgdeveloper.assistantcore.service;

import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class PdfAnalysisService {
    
    public String extractTextFromPdf(File pdfFile) {
        return "Extracted text from PDF";
    }
    
    public String analyzePdf(File pdfFile) {
        return "PDF analysis result";
    }
}