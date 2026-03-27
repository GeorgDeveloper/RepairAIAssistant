package ru.georgdeveloper.assistantyandexbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.georgdeveloper.assistantyandexbot.service.YandexBotService;

/**
 * Вебхук Bot API: тело запроса совпадает с ответом {@code getUpdates}
 * (<a href="https://yandex.ru/dev/messenger/doc/ru/api-requests/update-webhook">документация</a>).
 * Ответ должен быть быстрым (у платформы жёсткие таймауты), обработка — асинхронно.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

	private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

	private final YandexBotService yandexBotService;

	public WebhookController(YandexBotService yandexBotService) {
		this.yandexBotService = yandexBotService;
	}

	@PostMapping("/yandex")
	public ResponseEntity<Void> handleYandex(@RequestBody String body) {
		if (log.isDebugEnabled()) {
			log.debug("Yandex webhook, length={}", body != null ? body.length() : 0);
		}
		yandexBotService.handleWebhookPayloadAsync(body);
		return ResponseEntity.ok().build();
	}
}
