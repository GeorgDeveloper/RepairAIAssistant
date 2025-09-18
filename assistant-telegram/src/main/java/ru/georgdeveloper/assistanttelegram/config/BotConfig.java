package ru.georgdeveloper.assistanttelegram.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.georgdeveloper.assistanttelegram.bot.RepairAssistantBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableAsync
public class BotConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);
    
    @Autowired
    private RepairAssistantBot repairAssistantBot;
    
    private TelegramBotsApi telegramBotsApi;
    private boolean botRegistered = false;
    
    @Bean
    public TelegramBotsApi telegramBotsApi() {
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            registerBotWithRetry();
            return telegramBotsApi;
        } catch (Exception e) {
            logger.error("Ошибка инициализации Telegram API: {}", e.getMessage());
            // Возвращаем API без регистрации бота для продолжения работы приложения
            try {
                return new TelegramBotsApi(DefaultBotSession.class);
            } catch (TelegramApiException ex) {
                logger.error("Критическая ошибка создания TelegramBotsApi: {}", ex.getMessage());
                return null;
            }
        }
    }
    
    @Async
    public void registerBotWithRetry() {
        int maxRetries = 5;
        int retryDelay = 30000; // 30 секунд
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Попытка {} регистрации Telegram бота...", attempt);
                telegramBotsApi.registerBot(repairAssistantBot);
                botRegistered = true;
                logger.info("Telegram бот успешно зарегистрирован");
                return;
            } catch (Exception e) {
                logger.warn("Попытка {} не удалась: {}. Повтор через {} сек.", 
                    attempt, e.getMessage(), retryDelay / 1000);
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Прервана попытка переподключения");
                        return;
                    }
                }
            }
        }
        logger.error("Не удалось зарегистрировать Telegram бота после {} попыток", maxRetries);
    }
    
    // Периодическая проверка состояния бота каждые 5 минут
    @Scheduled(fixedRate = 300000)
    public void checkBotStatus() {
        if (!botRegistered && telegramBotsApi != null) {
            logger.info("Обнаружен незарегистрированный бот, попытка переподключения...");
            registerBotWithRetry();
        }
    }
}