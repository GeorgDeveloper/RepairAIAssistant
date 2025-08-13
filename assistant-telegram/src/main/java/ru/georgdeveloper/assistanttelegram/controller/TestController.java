package ru.georgdeveloper.assistanttelegram.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistanttelegram.service.TelegramBotService;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private TelegramBotService telegramBotService;
    
    @GetMapping("/message")
    public String testMessage(@RequestParam(defaultValue = "Найди самые продолжительные ремонты") String message) {
        Long testChatId = 123456789L;
        telegramBotService.processUpdate(message, testChatId);
        return "Сообщение обработано: " + message;
    }
    
    @GetMapping("/start")
    public String testStart() {
        Long testChatId = 123456789L;
        telegramBotService.processUpdate("/start", testChatId);
        return "Команда /start выполнена";
    }
}