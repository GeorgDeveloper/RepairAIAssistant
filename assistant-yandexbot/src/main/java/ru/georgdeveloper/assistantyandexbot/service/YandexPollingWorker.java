package ru.georgdeveloper.assistantyandexbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistantyandexbot.client.YandexMessengerClient;
import ru.georgdeveloper.assistantyandexbot.config.YandexBotProperties;

/**
 * Альтернатива вебхуку: polling {@code getUpdates}
 * (<a href="https://yandex.ru/dev/messenger/doc/ru/api-requests/update-polling">документация</a>).
 */
@Component
@ConditionalOnProperty(prefix = "yandex.bot.polling", name = "enabled", havingValue = "true")
public class YandexPollingWorker {

	private static final Logger log = LoggerFactory.getLogger(YandexPollingWorker.class);

	private final YandexMessengerClient messengerClient;
	private final YandexBotService yandexBotService;
	private final YandexBotProperties properties;

	private volatile int nextOffset = 0;

	public YandexPollingWorker(YandexMessengerClient messengerClient,
			YandexBotService yandexBotService,
			YandexBotProperties properties) {
		this.messengerClient = messengerClient;
		this.yandexBotService = yandexBotService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${yandex.bot.polling.interval-ms:1500}")
	public void poll() {
		if (properties.getToken() == null || properties.getToken().isBlank()) {
			return;
		}
		try {
			JsonNode root = messengerClient.getUpdates(100, nextOffset);
			if (!root.path("ok").asBoolean(false)) {
				log.warn("getUpdates: {}", root);
				return;
			}
			JsonNode updates = root.path("updates");
			if (!updates.isArray() || updates.isEmpty()) {
				return;
			}
			int maxId = nextOffset - 1;
			for (JsonNode u : updates) {
				yandexBotService.processUpdate(u);
				maxId = Math.max(maxId, u.path("update_id").asInt(0));
			}
			if (maxId >= 0) {
				nextOffset = maxId + 1;
			}
		} catch (Exception e) {
			log.error("Ошибка polling: {}", e.getMessage(), e);
		}
	}
}
