package ru.georgdeveloper.assistanttelegram.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistanttelegram.handler.MessageHandler;
import ru.georgdeveloper.assistanttelegram.handler.CommandHandler;


@Service
public class TelegramBotService {
    
    @Autowired
    private MessageHandler messageHandler;
    
    @Autowired
    private CommandHandler commandHandler;
    

    
    public void processUpdate(String message, Long chatId) {
        String response;
        
        if (commandHandler.isCommand(message)) {
            response = commandHandler.processCommand(message, chatId);
        } else {
            response = messageHandler.processMessage(message, chatId);
        }
        
        sendMessage(chatId, response);
    }
    
    private void sendMessage(Long chatId, String text) {
        // Split long messages for pagination
        if (text.length() > 4096) {
            sendPaginatedMessage(chatId, text);
        } else {
            messageHandler.sendMessage(chatId, text);
        }
    }
    
    private void sendPaginatedMessage(Long chatId, String text) {
        int maxLength = 4000;
        String[] parts = text.split("\n\n");
        StringBuilder currentMessage = new StringBuilder();
        int partIndex = 1;
        
        for (String part : parts) {
            if (currentMessage.length() + part.length() > maxLength) {
                messageHandler.sendMessage(chatId, currentMessage.toString() + 
                    String.format("\n\n📄 Часть %d", partIndex++));
                currentMessage = new StringBuilder(part);
            } else {
                if (currentMessage.length() > 0) {
                    currentMessage.append("\n\n");
                }
                currentMessage.append(part);
            }
        }
        
        if (currentMessage.length() > 0) {
            messageHandler.sendMessage(chatId, currentMessage.toString());
        }
    }
}