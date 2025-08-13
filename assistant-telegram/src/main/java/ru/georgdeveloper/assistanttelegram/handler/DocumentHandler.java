package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.stereotype.Component;

@Component
public class DocumentHandler {
    
    public String processDocument(String fileId, String fileName, Long chatId) {
        // TODO: Download and process document from Telegram
        return "Документ " + fileName + " получен и обрабатывается...";
    }
    
    public boolean isPdfDocument(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }
    
    public String downloadFile(String fileId) {
        // TODO: Implement Telegram file download
        return "file_path_" + fileId;
    }
}