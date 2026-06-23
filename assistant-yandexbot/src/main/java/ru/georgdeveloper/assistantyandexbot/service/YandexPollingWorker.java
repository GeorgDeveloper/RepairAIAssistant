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
 * Polling {@code getUpdates} с персистентным offset.
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
		long offset = offsetStore.getNextOffset();
		boolean draining = offsetStore.isDrainingBacklog();
		int batchLimit = draining ? 1000 : 100;
		try {
			JsonNode root = messengerClient.getUpdates(batchLimit, offset);
			if (!root.path("ok").asBoolean(false)) {
				log.warn("getUpdates: {}", root);
				return;
			}
			JsonNode updates = root.path("updates");
			if (!updates.isArray() || updates.isEmpty()) {
				if (offsetStore.isDrainingBacklog()) {
					offsetStore.finishBacklogDrain();
				}
				return;
			}

			boolean drainOnly = draining;
			long maxReplayAgeSec = properties.getPolling().getMaxReplayAgeSeconds();
			long nowEpochSec = System.currentTimeMillis() / 1000L;

			if (drainOnly) {
				log.info("Сброс очереди: подтверждаем {} апдейтов без ответов (offset={})", updates.size(), offset);
			}

			for (JsonNode u : updates) {
				long updateId = readUpdateId(u);
				try {
					if (drainOnly) {
						if (log.isDebugEnabled()) {
							log.debug("Сброс очереди (без ответа): update_id={}", updateId);
						}
					} else if (shouldSkipStaleUpdate(u, maxReplayAgeSec, nowEpochSec)) {
						log.info("Пропуск устаревшего update_id={} (timestamp={})", updateId, u.path("timestamp").asLong(0));
					} else {
						yandexBotService.processUpdate(u);
					}
				} catch (Exception e) {
					log.error("Ошибка обработки update_id={}: {}", updateId, e.getMessage(), e);
				} finally {
					offsetStore.advanceTo(updateId);
				}
			}
		} catch (Exception e) {
			log.error("Ошибка polling: {}", e.getMessage(), e);
		}
	}

	/** API отдаёт update_id как int32; большие значения приходят отрицательными. */
	static long readUpdateId(JsonNode update) {
		JsonNode id = update.path("update_id");
		if (id.isMissingNode() || id.isNull()) {
			return 0L;
		}
		return id.asLong();
	}

	private static boolean shouldSkipStaleUpdate(JsonNode update, long maxReplayAgeSec, long nowEpochSec) {
		if (maxReplayAgeSec <= 0) {
			return false;
		}
		long ts = update.path("timestamp").asLong(0);
		if (ts <= 0) {
			return false;
		}
		long age = nowEpochSec - ts;
		return age > maxReplayAgeSec || age < -300L;
	}
}
