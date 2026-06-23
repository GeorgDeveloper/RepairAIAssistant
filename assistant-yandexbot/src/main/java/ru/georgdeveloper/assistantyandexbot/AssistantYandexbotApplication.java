package ru.georgdeveloper.assistantyandexbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AssistantYandexbotApplication {

	private static final Logger log = LoggerFactory.getLogger(AssistantYandexbotApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AssistantYandexbotApplication.class, args);
		log.info("Yandex Messenger bot module started (Bot API: https://yandex.ru/dev/messenger/doc/ru/)");
	}
}
