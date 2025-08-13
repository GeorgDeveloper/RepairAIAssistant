package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistanttelegram.client.CoreServiceClient;

@Component
public class MessageHandler {
    
    @Autowired
    private CoreServiceClient coreServiceClient;
    
    public String processMessage(String message, Long chatId) {
        return coreServiceClient.analyzeRepairRequest(message);
    }
    
    public void sendMessage(Long chatId, String text) {
        System.out.println("Отправка сообщения в чат " + chatId + ": " + text);
    }
}