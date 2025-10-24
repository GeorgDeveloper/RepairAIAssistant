package ru.georgdeveloper.assistantbaseupdate.util;

import org.slf4j.Logger;
import org.springframework.dao.CannotAcquireLockException;

import java.util.function.Supplier;

/**
 * Утилитарный класс для выполнения операций с повторными попытками при deadlock.
 */
public final class RetryExecutor {
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY = 1000; // 1 секунда
    
    private RetryExecutor() {
        // Utility class
    }
    
    /**
     * Выполняет операцию с повторными попытками при deadlock
     * @param operation операция для выполнения
     * @param operationName название операции для логирования
     * @param logger логгер для записи сообщений
     * @return результат выполнения операции
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName, Logger logger) {
        return executeWithRetry(operation, operationName, logger, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY);
    }
    
    /**
     * Выполняет операцию с повторными попытками при deadlock
     * @param operation операция для выполнения
     * @param operationName название операции для логирования
     * @param logger логгер для записи сообщений
     * @param maxRetries максимальное количество попыток
     * @param initialDelay начальная задержка между попытками в миллисекундах
     * @return результат выполнения операции
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName, Logger logger, 
                                       int maxRetries, int initialDelay) {
        int retryDelay = initialDelay;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (CannotAcquireLockException e) {
                if (attempt == maxRetries) {
                    logger.error("Критическая ошибка при {}: исчерпаны все попытки ({})", operationName, maxRetries);
                    throw e;
                }
                logger.warn("Deadlock при {} (попытка {}/{}), повтор через {} мс", 
                           operationName, attempt, maxRetries, retryDelay);
                
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано во время ожидания повтора", ie);
                }
                retryDelay *= 2; // Экспоненциальная задержка
            }
        }
        
        // Этот код никогда не должен выполняться, но компилятор требует return
        throw new RuntimeException("Неожиданная ошибка в RetryExecutor");
    }
}
