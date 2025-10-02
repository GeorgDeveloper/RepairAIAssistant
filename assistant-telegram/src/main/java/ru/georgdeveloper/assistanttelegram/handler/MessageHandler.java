package ru.georgdeveloper.assistanttelegram.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistanttelegram.client.CoreServiceClient;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    @Autowired
    private CoreServiceClient coreServiceClient;

    public CoreServiceClient getCoreServiceClient() {
        return coreServiceClient;
    }
    

    
    public String processMessage(String message, Long chatId) {
        return processMessage(message, chatId, null);
    }
    
    public String processMessage(String message, Long chatId, ProgressCallback callback) {
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Запускаем периодическое обновление индикатора
        Future<?> progressTask = executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (callback != null) {
                        callback.updateProgress();
                    }
                    Thread.sleep(4000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Запускаем основную задачу
        Future<String> mainTask = executor.submit(() -> {
            return coreServiceClient.analyzeRepairRequest(message);
        });
        
        try {
            // Ожидаем результат максимум 4 минуты
            String result = mainTask.get(4, TimeUnit.MINUTES);
            return result;
        } catch (TimeoutException e) {
            logger.warn("Таймаут: помощник не ответил за 4 минуты для чата {}", chatId);
            mainTask.cancel(true);
            return "⏰ Запрос обрабатывается слишком долго. Попробуйте упростить вопрос или повторить позже.";
        } catch (ExecutionException e) {
            logger.error("Ошибка выполнения задачи для чата {}: {}", chatId, e.getMessage(), e);
            Throwable cause = e.getCause();
            if (cause instanceof org.springframework.web.client.HttpClientErrorException) {
                return "❌ Сервис временно недоступен. Попробуйте позже.";
            } else if (cause instanceof org.springframework.web.client.ResourceAccessException) {
                return "❌ Нет связи с сервисом. Обратитесь к администратору.";
            } else {
                return "❌ Произошла ошибка при обработке запроса. Попробуйте позже.";
            }
        } catch (Exception e) {
            logger.error("Неожиданная ошибка обработки для чата {}: {}", chatId, e.getMessage(), e);
            return "❌ Произошла неожиданная ошибка. Попробуйте перезапустить диалог командой /start";
        } finally {
            progressTask.cancel(true);
            executor.shutdownNow();
        }
    }
    
    public void sendMessage(Long chatId, String text) {
        logger.info("Отправка сообщения в чат {}: {}", chatId, text);
    }
    
    @FunctionalInterface
    public interface ProgressCallback {
        void updateProgress();
    }
}