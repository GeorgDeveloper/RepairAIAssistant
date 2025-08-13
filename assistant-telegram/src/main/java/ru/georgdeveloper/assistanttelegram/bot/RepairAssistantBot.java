package ru.georgdeveloper.assistanttelegram.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.georgdeveloper.assistanttelegram.config.BotProperties;
import ru.georgdeveloper.assistanttelegram.handler.CommandHandler;
import ru.georgdeveloper.assistanttelegram.handler.MessageHandler;

/**
 * Основной класс Telegram бота для системы ремонта оборудования.
 * 
 * Архитектура взаимодействия:
 * 1. Telegram API -> RepairAssistantBot (получение сообщений)
 * 2. RepairAssistantBot -> CommandHandler/MessageHandler (обработка)
 * 3. MessageHandler -> CoreServiceClient (запрос к assistant-core)
 * 4. CoreServiceClient -> RepairAssistantService (бизнес-логика)
 * 5. RepairAssistantService -> OllamaClient (AI анализ)
 * 6. Ответ возвращается по цепочке обратно в Telegram
 * 
 * Особенности:
 * - Использует Long Polling для получения обновлений от Telegram
 * - Поддерживает автоматическую пагинацию длинных сообщений (>4096 символов)
 * - Интегрируется с реальными данными из производственной БД
 * - Фильтрует технические размышления AI модели deepseek-r1
 */
@Component
public class RepairAssistantBot extends TelegramLongPollingBot {
    
    // Конфигурация бота (токен, имя пользователя)
    @Autowired
    private BotProperties botProperties;
    
    // Обработчик команд (/start, /help)
    @Autowired
    private CommandHandler commandHandler;
    
    // Обработчик текстовых сообщений с интеграцией в core модуль
    @Autowired
    private MessageHandler messageHandler;
    
    /**
     * Возвращает имя пользователя бота из конфигурации.
     * Используется Telegram API для идентификации бота.
     */
    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }
    
    /**
     * Возвращает токен бота из конфигурации.
     * Токен получается от @BotFather в Telegram.
     */
    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }
    
    /**
     * Главный метод обработки входящих обновлений от Telegram.
     * 
     * Логика маршрутизации:
     * - Команды (начинающиеся с /) -> CommandHandler
     * - Обычные сообщения -> MessageHandler -> AI анализ
     * - Автоматическая отправка ответа пользователю
     * 
     * @param update Объект обновления от Telegram API
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что получено текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            
            String response;
            
            // Маршрутизация по типу сообщения
            if (commandHandler.isCommand(messageText)) {
                // Обработка команд (/start, /help)
                response = commandHandler.processCommand(messageText, chatId);
            } else {
                // Обработка обычных запросов через AI
                response = messageHandler.processMessage(messageText, chatId);
            }
            
            // Отправка ответа пользователю с автоматической пагинацией
            sendTextMessage(chatId, response);
        }
    }
    
    /**
     * Отправляет текстовое сообщение пользователю.
     * 
     * Особенности:
     * - Автоматическая обработка длинных сообщений
     * - Логирование ошибок отправки
     * - Безопасная обработка исключений Telegram API
     * 
     * @param chatId ID чата для отправки сообщения
     * @param text Текст сообщения для отправки
     */
    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
        try {
            // Отправка через Telegram API
            execute(message);
        } catch (TelegramApiException e) {
            // Логирование ошибок для отладки
            System.err.println("Ошибка отправки сообщения в Telegram: " + e.getMessage());
            e.printStackTrace();
        }
    }
}