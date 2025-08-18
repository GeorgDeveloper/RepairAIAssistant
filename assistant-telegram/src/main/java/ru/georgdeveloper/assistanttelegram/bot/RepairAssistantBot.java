package ru.georgdeveloper.assistanttelegram.bot;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.ActionType;
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
 * - Фильтрует технические размышления AI модели deepseek-coder:6.7b
 */
@Component
public class RepairAssistantBot extends TelegramLongPollingBot {
    
    private final BotProperties botProperties;
    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    
    public RepairAssistantBot(BotProperties botProperties, CommandHandler commandHandler, MessageHandler messageHandler) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.commandHandler = commandHandler;
        this.messageHandler = messageHandler;
    }
    
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
                // Показываем индикатор "печатает"
                sendTypingAction(chatId);
                
                // Обработка обычных запросов через AI с периодическим обновлением индикатора
                response = messageHandler.processMessage(messageText, chatId, () -> sendTypingAction(chatId));
            }
            
            // Отправка ответа пользователю с автоматической пагинацией
            sendTextMessage(chatId, response);
        }
    }
    
    /**
     * Отправляет индикатор "печатает" в чат
     */
    private void sendTypingAction(Long chatId) {
        SendChatAction chatAction = new SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(ActionType.TYPING);
        
        try {
            execute(chatAction);
            System.out.println("Индикатор 'печатает' отправлен в чат " + chatId);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки индикатора печати: " + e.getMessage());
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