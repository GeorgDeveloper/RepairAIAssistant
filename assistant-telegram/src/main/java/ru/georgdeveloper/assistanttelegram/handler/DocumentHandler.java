package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgdeveloper.assistanttelegram.service.TelegramBotService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class DocumentHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentHandler.class);
    
    @Autowired
    private TelegramBotService telegramBotService;
    
    private static final String DOWNLOAD_DIR = "downloads";
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    
    public String processDocument(String fileId, String fileName, Long chatId) {
        try {
            logger.info("Processing document: {} with fileId: {} for chat: {}", fileName, fileId, chatId);
            
            // Download the file from Telegram
            String filePath = downloadFile(fileId);
            if (filePath == null) {
                return "Ошибка: не удалось загрузить документ. Попробуйте еще раз.";
            }
            
            // Check file type and process accordingly
            if (isPdfDocument(fileName)) {
                return processPdfDocument(filePath, fileName, chatId);
            } else {
                return processOtherDocument(filePath, fileName, chatId);
            }
            
        } catch (Exception e) {
            logger.error("Error processing document {}: {}", fileName, e.getMessage(), e);
            return "Ошибка при обработке документа: " + e.getMessage();
        }
    }
    
    public boolean isPdfDocument(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }
    
    public String downloadFile(String fileId) {
        try {
            // Get file information from Telegram
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            
            File file = telegramBotService.getBot().execute(getFile);
            
            if (file == null) {
                logger.error("File not found for fileId: {}", fileId);
                return null;
            }
            
            // Check file size
            if (file.getFileSize() != null && file.getFileSize() > MAX_FILE_SIZE) {
                logger.warn("File too large: {} bytes (max: {} bytes)", file.getFileSize(), MAX_FILE_SIZE);
                return null;
            }
            
            // Create download directory if it doesn't exist
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            if (!Files.exists(downloadPath)) {
                Files.createDirectories(downloadPath);
            }
            
            // Generate unique filename
            String fileExtension = getFileExtension(file.getFilePath());
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path localFilePath = downloadPath.resolve(uniqueFileName);
            
            // Download file from Telegram servers
            String fileUrl = telegramBotService.getBot().getBaseUrl() + "/file/bot" + 
                           telegramBotService.getBot().getBotToken() + "/" + file.getFilePath();
            
            try (InputStream inputStream = new java.net.URL(fileUrl).openStream()) {
                Files.copy(inputStream, localFilePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("File downloaded successfully: {}", localFilePath);
                return localFilePath.toString();
            }
            
        } catch (TelegramApiException e) {
            logger.error("Telegram API error downloading file {}: {}", fileId, e.getMessage(), e);
            return null;
        } catch (IOException e) {
            logger.error("IO error downloading file {}: {}", fileId, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error downloading file {}: {}", fileId, e.getMessage(), e);
            return null;
        }
    }
    
    private String processPdfDocument(String filePath, String fileName, Long chatId) {
        try {
            logger.info("Processing PDF document: {}", fileName);
            
            // For now, return a simple message about PDF processing
            // In a real implementation, you would use a PDF library like Apache PDFBox
            // to extract text content and send it to the AI service
            
            return "PDF документ '" + fileName + "' получен. " +
                   "Функция анализа PDF документов находится в разработке. " +
                   "Пожалуйста, отправьте текст вопроса напрямую.";
                   
        } catch (Exception e) {
            logger.error("Error processing PDF document {}: {}", fileName, e.getMessage(), e);
            return "Ошибка при обработке PDF документа: " + e.getMessage();
        }
    }
    
    private String processOtherDocument(String filePath, String fileName, Long chatId) {
        try {
            logger.info("Processing other document: {}", fileName);
            
            // For non-PDF documents, we can try to read as text if it's a text file
            String fileExtension = getFileExtension(fileName);
            
            if (isTextFile(fileExtension)) {
                return processTextDocument(filePath, fileName, chatId);
            } else {
                return "Документ '" + fileName + "' получен. " +
                       "Поддерживается только обработка текстовых файлов и PDF. " +
                       "Пожалуйста, отправьте текст вопроса напрямую.";
            }
            
        } catch (Exception e) {
            logger.error("Error processing document {}: {}", fileName, e.getMessage(), e);
            return "Ошибка при обработке документа: " + e.getMessage();
        }
    }
    
    private String processTextDocument(String filePath, String fileName, Long chatId) {
        try {
            // Read text content from file
            String content = Files.readString(Paths.get(filePath));
            
            if (content.trim().isEmpty()) {
                return "Файл '" + fileName + "' пуст или не содержит текста.";
            }
            
            // Send content to AI service for analysis
            // This would integrate with the existing message processing
            return "Текстовый документ '" + fileName + "' получен и проанализирован. " +
                   "Содержимое: " + (content.length() > 500 ? content.substring(0, 500) + "..." : content);
                   
        } catch (IOException e) {
            logger.error("Error reading text file {}: {}", fileName, e.getMessage(), e);
            return "Ошибка при чтении текстового файла: " + e.getMessage();
        }
    }
    
    private boolean isTextFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals(".txt") || ext.equals(".log") || ext.equals(".md") || 
               ext.equals(".json") || ext.equals(".xml") || ext.equals(".csv");
    }
    
    private String getFileExtension(String filePath) {
        if (filePath == null || !filePath.contains(".")) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf("."));
    }
}