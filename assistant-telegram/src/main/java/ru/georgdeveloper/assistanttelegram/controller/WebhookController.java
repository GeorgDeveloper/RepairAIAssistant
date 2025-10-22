package ru.georgdeveloper.assistanttelegram.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.georgdeveloper.assistanttelegram.service.TelegramBotService;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private TelegramBotService telegramBotService;
    
    @PostMapping("/telegram")
    public void handleTelegramUpdate(@RequestBody String updateJson) {
        try {
            // Parse Telegram update JSON
            Update update = objectMapper.readValue(updateJson, Update.class);
            
            // Extract message and chat ID from the update
            if (update.hasMessage()) {
                Long chatId = update.getMessage().getChatId();
                
                if (update.getMessage().hasText()) {
                    String message = update.getMessage().getText();
                    logger.info("Received text message from chat {}: {}", chatId, message);
                    telegramBotService.processUpdate(message, chatId);
                } else if (update.getMessage().hasDocument()) {
                    String fileId = update.getMessage().getDocument().getFileId();
                    String fileName = update.getMessage().getDocument().getFileName();
                    logger.info("Received document from chat {}: {} (fileId: {})", chatId, fileName, fileId);
                    telegramBotService.processDocument(fileId, fileName, chatId);
                } else {
                    logger.warn("Received message without text or document from chat {}: {}", chatId, updateJson);
                }
            } else if (update.hasCallbackQuery()) {
                // Handle callback queries (inline keyboard buttons)
                String callbackData = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                
                logger.info("Received callback query from chat {}: {}", chatId, callbackData);
                telegramBotService.processUpdate(callbackData, chatId);
            } else {
                logger.warn("Received update without text message, document, or callback query: {}", updateJson);
            }
        } catch (Exception e) {
            logger.error("Error parsing Telegram update: {}", e.getMessage(), e);
        }
    }
}