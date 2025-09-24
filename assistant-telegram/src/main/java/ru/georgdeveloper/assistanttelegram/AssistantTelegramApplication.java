package ru.georgdeveloper.assistanttelegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
public class AssistantTelegramApplication {
    // Логгер приложения Telegram-бота
    private static final Logger logger = LoggerFactory.getLogger(AssistantTelegramApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AssistantTelegramApplication.class, args);
        // Информационные сообщения о запуске через логгер
        logger.info("🤖 Telegram Bot запущен на порту 8082");
        logger.info("🔗 Bot подключен к Telegram API");
	}
}
