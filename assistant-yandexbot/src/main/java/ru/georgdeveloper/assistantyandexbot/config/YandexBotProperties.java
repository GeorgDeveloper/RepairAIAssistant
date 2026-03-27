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
	}
}
