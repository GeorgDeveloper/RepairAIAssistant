package ru.georgdeveloper.assistanttelegram.bot;


import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String response;
            if (commandHandler.isCommand(messageText)) {
                response = commandHandler.processCommand(messageText, chatId);
                sendTextMessage(chatId, response);
            } else {
                sendTypingAction(chatId);
                response = messageHandler.processMessage(messageText, chatId, () -> sendTypingAction(chatId));
                sendTextMessageWithFeedback(chatId, messageText, response);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    /**
     * Отправляет сообщение с Inline-кнопками для обратной связи
     */
    private void sendTextMessageWithFeedback(Long chatId, String userQuery, String answer) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(answer);

        // Формируем короткий идентификатор для пары запрос-ответ
        String feedbackId = shortHash(userQuery + "::" + answer);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton btnCorrect = new InlineKeyboardButton("✅ Ответ корректен");
        btnCorrect.setCallbackData("feedback_correct::" + feedbackId);
        InlineKeyboardButton btnRetry = new InlineKeyboardButton("🔄 Сгенерировать заново");
        btnRetry.setCallbackData("feedback_retry::" + feedbackId);
        InlineKeyboardButton btnNew = new InlineKeyboardButton("🆕 Новый диалог");
        btnNew.setCallbackData("feedback_new");
        markup.setKeyboard(java.util.Arrays.asList(
            java.util.Arrays.asList(btnCorrect),
            java.util.Arrays.asList(btnRetry, btnNew)
        ));
        message.setReplyMarkup(markup);
        // Сохраняем временно в памяти соответствие feedbackId -> запрос/ответ
        FeedbackMemory.put(feedbackId, new FeedbackPair(userQuery, answer));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения с кнопками: " + e.getMessage());
        }
    }

    /**
     * Обработка обратной связи пользователя через CallbackQuery
     */
    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        if (data.startsWith("feedback_correct::")) {
            String feedbackId = data.substring("feedback_correct::".length());
            FeedbackPair pair = FeedbackMemory.get(feedbackId);
            if (pair != null) {
                // Сохраняем пару запрос-ответ через CoreServiceClient
                try {
                    messageHandler.getCoreServiceClient().saveFeedback(pair.userQuery, pair.answer);
                    sendTextMessage(chatId, "Спасибо! Ответ сохранён для дообучения модели. Можете задать новый вопрос.");
                } catch (Exception e) {
                    sendTextMessage(chatId, "Ошибка при сохранении обратной связи: " + e.getMessage());
                }
            } else {
                sendTextMessage(chatId, "Ошибка: не удалось найти исходный запрос/ответ.");
            }
        } else if (data.startsWith("feedback_retry::")) {
            String feedbackId = data.substring("feedback_retry::".length());
            FeedbackPair pair = FeedbackMemory.get(feedbackId);
            if (pair != null) {
                sendTypingAction(chatId);
                String newAnswer = messageHandler.processMessage(pair.userQuery, chatId, () -> sendTypingAction(chatId));
                sendTextMessageWithFeedback(chatId, pair.userQuery, newAnswer);
            } else {
                sendTextMessage(chatId, "Ошибка: не удалось найти исходный запрос.");
            }
        } else if (data.equals("feedback_new")) {
            sendTextMessage(chatId, "Диалог сброшен. Можете задать новый вопрос.");
        }
    }

    // Временное хранилище соответствий feedbackId -> запрос/ответ
    private static final Map<String, FeedbackPair> FeedbackMemory = new java.util.concurrent.ConcurrentHashMap<>();
    private static class FeedbackPair {
        final String userQuery;
        final String answer;
        FeedbackPair(String userQuery, String answer) {
            this.userQuery = userQuery;
            this.answer = answer;
        }
    }

    // Короткий hash для callback_data (Base64 от первых 8 байт SHA-256)
    private String shortHash(String s) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOf(hash, 8));
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
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