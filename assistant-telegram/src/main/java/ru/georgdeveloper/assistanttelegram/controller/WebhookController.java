package ru.georgdeveloper.assistanttelegram.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistanttelegram.service.TelegramBotService;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    @Autowired
    private TelegramBotService telegramBotService;
    
    @PostMapping("/telegram")
    public void handleTelegramUpdate(@RequestBody String update) {
        // TODO: Parse Telegram update JSON
        // For now, simulate message processing
        Long chatId = 123456789L;
        String message = extractMessage(update);
        
        telegramBotService.processUpdate(message, chatId);
    }
    
    private String extractMessage(String update) {
        // TODO: Implement proper JSON parsing
        return "Найди самые продолжительные ремонты";
    }
}