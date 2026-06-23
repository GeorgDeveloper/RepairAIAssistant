package ru.georgdeveloper.assistantyandexbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "yandex.bot")
public class YandexBotProperties {

	/**
	 * OAuth-токен бота (заголовок {@code Authorization: OAuth &lt;token&gt;}).
	 */
	private String token;

	private final Polling polling = new Polling();

	@Data
	public static class Polling {
		private boolean enabled;
		private long intervalMs = 1500;
		/** Файл с последним подтверждённым offset (max(update_id)+1). */
		private String offsetFile = "./data/yandex-polling.offset";
		/**
		 * Не выполнять бизнес-логику для обновлений старше N секунд (защита при потере offset-файла).
		 * 0 — отключено.
		 */
		private long maxReplayAgeSeconds = 3600;
		/**
		 * При каждом старте бота: сначала подтвердить всю накопленную очередь getUpdates
		 * без ответов пользователям (сброс кликов, пока бот был недоступен).
		 */
		private boolean drainBacklogOnStartup = true;
	}
}
