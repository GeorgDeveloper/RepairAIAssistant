package ru.georgdeveloper.assistanttelegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AssistantTelegramApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistantTelegramApplication.class, args);
		System.out.println("🤖 Telegram Bot запущен на порту 8082");
		System.out.println("🔗 Bot подключен к Telegram API");
	}
}
