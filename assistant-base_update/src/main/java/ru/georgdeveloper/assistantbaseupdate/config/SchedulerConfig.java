package ru.georgdeveloper.assistantbaseupdate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.PlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Конфигурация планировщика задач (Scheduler) для обеспечения правильного управления транзакциями.
 * 
 * Проблема:
 * По умолчанию Spring использует single-threaded scheduler, который может не гарантировать
 * правильную фиксацию (commit) транзакций в многопоточной среде.
 * 
 * Решение:
 * Явно конфигурируем ThreadPoolTaskScheduler с:
 * - Несколькими потоками (пул потоков)
 * - Правильным управлением жизненным циклом потоков
 * - Гарантией завершения задач при выключении приложения
 * - Специальной политикой отклонения задач (CallerRunsPolicy)
 * 
 * Это гарантирует, что @Scheduled методы правильно участвуют в транзакциях Spring.
 */
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
    
    private final PlatformTransactionManager transactionManager;

    public SchedulerConfig(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        logger.info("SchedulerConfig инициализирован с PlatformTransactionManager: {}", 
                    transactionManager.getClass().getSimpleName());
    }

    /**
     * Создание и конфигурация ThreadPoolTaskScheduler для планируемых задач.
     * 
     * Параметры:
     * - poolSize: 5 потоков для параллельного выполнения независимых задач
     * - threadNamePrefix: "pm-scheduler-" для удобной идентификации в логах
     * - awaitTerminationSeconds: 60 секунд ожидания завершения задач
     * - waitForTasksToCompleteOnShutdown: true - дождаться завершения перед выключением
     * - rejectedExecutionHandler: CallerRunsPolicy - выполнять в потоке caller если пул переполнен
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 5 потоков для параллельного выполнения
        scheduler.setThreadNamePrefix("pm-scheduler-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setRemoveOnCancelPolicy(true);  // Удалять отменённые задачи из очереди
        
        logger.info("ThreadPoolTaskScheduler создан: poolSize=5, threadNamePrefix='pm-scheduler-'");
        
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Регистрирует наш ThreadPoolTaskScheduler в качестве executor для всех @Scheduled методов.
     * 
     * Это гарантирует, что @Scheduled методы будут выполняться в контексте правильного thread pool,
     * что критически важно для управления транзакциями.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        logger.info("Конфигурирование ScheduledTaskRegistrar с пользовательским TaskScheduler");
        taskRegistrar.setScheduler(taskScheduler());
    }
}
