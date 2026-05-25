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
 *
 * <p>Алгоритм полностью следует рекомендации API:
 * берем пачку, обрабатываем, затем двигаем {@code offset = max(update_id)+1}.
 * Offset сохраняется на диск после каждого обновления, чтобы перезапуск не воспроизводил очередь.
 */
@Component
@ConditionalOnProperty(prefix = "yandex.bot.polling", name = "enabled", havingValue = "true")
public class YandexPollingWorker {

	private static final Logger log = LoggerFactory.getLogger(YandexPollingWorker.class);

	private final YandexMessengerClient messengerClient;
	private final YandexBotService yandexBotService;
	private final YandexBotProperties properties;
	private final YandexPollingOffsetStore offsetStore;

	public YandexPollingWorker(YandexMessengerClient messengerClient,
			YandexBotService yandexBotService,
			YandexBotProperties properties,
			YandexPollingOffsetStore offsetStore) {
		this.messengerClient = messengerClient;
		this.yandexBotService = yandexBotService;
		this.properties = properties;
		this.offsetStore = offsetStore;
	}

	@Scheduled(fixedDelayString = "${yandex.bot.polling.interval-ms:1500}")
	public void poll() {
		if (properties.getToken() == null || properties.getToken().isBlank()) {
			return;
		}
		int offset = offsetStore.getNextOffset();
		try {
			JsonNode root = messengerClient.getUpdates(100, offset);
			if (!root.path("ok").asBoolean(false)) {
				log.warn("getUpdates: {}", root);
				return;
			}
			JsonNode updates = root.path("updates");
			if (!updates.isArray() || updates.isEmpty()) {
				return;
			}
			long maxReplayAgeSec = properties.getPolling().getMaxReplayAgeSeconds();
			long nowEpochSec = System.currentTimeMillis() / 1000L;

			for (JsonNode u : updates) {
				int updateId = u.path("update_id").asInt(0);
				try {
					if (shouldSkipStaleUpdate(u, maxReplayAgeSec, nowEpochSec)) {
						log.info("Пропуск устаревшего update_id={} (timestamp={})", updateId, u.path("timestamp").asLong(0));
					} else {
						yandexBotService.processUpdate(u);
					}
				} catch (Exception e) {
					log.error("Ошибка обработки update_id={}: {}", updateId, e.getMessage(), e);
				} finally {
					// Подтверждаем доставку даже при сбое обработки, иначе API отдаёт те же updates снова.
					offsetStore.advanceTo(updateId);
				}
			}
		} catch (Exception e) {
			log.error("Ошибка polling: {}", e.getMessage(), e);
		}
	}

	private static boolean shouldSkipStaleUpdate(JsonNode update, long maxReplayAgeSec, long nowEpochSec) {
		if (maxReplayAgeSec <= 0) {
			return false;
		}
		long ts = update.path("timestamp").asLong(0);
		if (ts <= 0) {
			return false;
		}
		return (nowEpochSec - ts) > maxReplayAgeSec;
	}
}
