package ru.georgdeveloper.assistantyandexbot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.georgdeveloper.assistantyandexbot.config.YandexBotProperties;
import ru.georgdeveloper.assistantyandexbot.model.YandexChatTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Клиент <a href="https://yandex.ru/dev/messenger/doc/ru/">Bot API Мессенджера Яндекс 360</a>.
 */
@Component
public class YandexMessengerClient {

	private static final Logger log = LoggerFactory.getLogger(YandexMessengerClient.class);
	public static final String API_ROOT = "https://botapi.messenger.yandex.net/bot/v1/";

	private final RestTemplate restTemplate;
	private final YandexBotProperties properties;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public YandexMessengerClient(RestTemplate restTemplate, YandexBotProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	private HttpHeaders oauthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "OAuth " + properties.getToken());
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private HttpHeaders oauthHeadersNoContentType() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "OAuth " + properties.getToken());
		return headers;
	}

	public void sendText(YandexChatTarget target, String text, Long replyMessageId,
			List<Map<String, Object>> inlineKeyboard) {
		if (properties.getToken() == null || properties.getToken().isBlank()) {
			log.warn("yandex.bot.token пуст — сообщение не отправлено");
			return;
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("text", text);
		body.put("payload_id", UUID.randomUUID().toString());
		if (replyMessageId != null) {
			body.put("reply_message_id", replyMessageId);
		}
		if (target.needsLogin()) {
			body.put("login", target.login());
		} else if (target.chatId() != null && !target.chatId().isBlank()) {
			body.put("chat_id", target.chatId());
		} else {
			log.warn("Нет ни chat_id, ни login для отправки сообщения");
			return;
		}
		if (inlineKeyboard != null && !inlineKeyboard.isEmpty()) {
			body.put("inline_keyboard", inlineKeyboard);
		}

		try {
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, oauthHeaders());
			ResponseEntity<String> response = restTemplate.exchange(
					API_ROOT + "messages/sendText/",
					HttpMethod.POST,
					entity,
					String.class);
			JsonNode root = objectMapper.readTree(response.getBody());
			if (!root.path("ok").asBoolean(false)) {
				log.warn("sendText неуспех: {}", response.getBody());
			}
		} catch (Exception e) {
			log.error("Ошибка sendText: {}", e.getMessage(), e);
		}
	}

	public JsonNode getUpdates(int limit, int offset) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("limit", limit);
		body.put("offset", offset);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, oauthHeaders());
		ResponseEntity<String> response = restTemplate.exchange(
				API_ROOT + "messages/getUpdates/",
				HttpMethod.POST,
				entity,
				String.class);
		return objectMapper.readTree(response.getBody());
	}

	public byte[] downloadFile(String fileId) {
		try {
			String url = UriComponentsBuilder.fromUriString(API_ROOT + "messages/getFile/")
					.queryParam("file_id", fileId)
					.encode()
					.build()
					.toUriString();
			HttpEntity<Void> entity = new HttpEntity<>(oauthHeadersNoContentType());
			ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
			return response.getBody();
		} catch (Exception e) {
			log.debug("GET getFile не удался, пробуем POST multipart: {}", e.getMessage());
		}

		try {
			MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
			multipart.add("file_id", fileId);
			HttpHeaders headers = oauthHeadersNoContentType();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(multipart, headers);
			ResponseEntity<byte[]> response = restTemplate.exchange(
					API_ROOT + "messages/getFile/",
					HttpMethod.POST,
					entity,
					byte[].class);
			return response.getBody();
		} catch (Exception e) {
			log.error("Ошибка getFile: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Регистрация webhook (опционально). POST /bot/v1/self/update/
	 *
	 * @see <a href="https://yandex.ru/dev/messenger/doc/ru/api-requests/update-webhook">update-webhook</a>
	 */
	public void setWebhookUrl(String webhookUrl) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("webhook_url", webhookUrl);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, oauthHeaders());
		restTemplate.exchange(API_ROOT + "self/update/", HttpMethod.POST, entity, String.class);
	}
}
