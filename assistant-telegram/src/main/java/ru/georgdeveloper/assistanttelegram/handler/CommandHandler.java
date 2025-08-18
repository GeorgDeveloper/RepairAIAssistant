package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.stereotype.Component;

@Component
public class CommandHandler {
    
    public String handleStart(Long chatId) {
        return "🔧 Добро пожаловать в Kvant AI!\n\n" +
               "Я помогу вам с вопросами по ремонту оборудования.\n\n" +
               "Вы можете:\n" +
               "• Задать вопрос о ремонте\n" +
               "• Найти самые продолжительные ремонты\n" +
               "• Получить советы по устранению неисправностей\n\n" +
               "Просто напишите ваш вопрос!";
    }
    
    public String handleHelp(Long chatId) {
        return "📋 Доступные команды:\n\n" +
               "/start - Начать работу с ботом\n" +
               "/help - Показать эту справку\n\n" +
               "💡 Примеры запросов:\n" +
               "• Найди самые продолжительные ремонты\n" +
               "• Как починить телевизор\n" +
               "• Проблемы с холодильником";
    }
    
    public boolean isCommand(String message) {
        return message != null && message.startsWith("/");
    }
    
    public String processCommand(String command, Long chatId) {
        switch (command) {
            case "/start":
                return handleStart(chatId);
            case "/help":
                return handleHelp(chatId);
            default:
                return "Неизвестная команда. Используйте /help для списка команд.";
        }
    }
}