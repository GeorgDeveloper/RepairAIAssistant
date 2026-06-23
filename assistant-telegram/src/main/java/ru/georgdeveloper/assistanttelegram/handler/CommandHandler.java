package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.Arrays;
import java.util.List;

@Component
public class CommandHandler {
    
    public String handleStart(Long chatId) {
        return "🔧 Добро пожаловать в Repair AI Assistant!\n\n" +
               "Я помогу вам с вопросами по ремонту оборудования и предоставлю отчеты по производственным показателям.\n\n" +
               "Выберите действие:";
    }
    
    public InlineKeyboardMarkup getMainMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        
        InlineKeyboardButton chatButton = new InlineKeyboardButton("💬 Чат с ассистентом");
        chatButton.setCallbackData("chat_with_assistant");
        
        InlineKeyboardButton reportButton = new InlineKeyboardButton("📊 Запрос отчета");
        reportButton.setCallbackData("request_report");
        
        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
            Arrays.asList(chatButton),
            Arrays.asList(reportButton)
        );
        
        markup.setKeyboard(keyboard);
        return markup;
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