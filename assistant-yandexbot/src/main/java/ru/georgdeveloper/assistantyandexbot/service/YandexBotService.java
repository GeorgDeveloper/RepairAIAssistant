package ru.georgdeveloper.assistantyandexbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantyandexbot.client.YandexMessengerClient;
import ru.georgdeveloper.assistantyandexbot.handler.CommandHandler;
import ru.georgdeveloper.assistantyandexbot.handler.DocumentHandler;
import ru.georgdeveloper.assistantyandexbot.handler.MessageHandler;
import ru.georgdeveloper.assistantyandexbot.handler.ReportHandler;
import ru.georgdeveloper.assistantyandexbot.model.YandexChatTarget;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Логика бота (аналог {@code RepairAssistantBot} в assistant-telegram) поверх Bot API Яндекс Мессенджера.
 */
@Service
public class YandexBotService {

	private static final Logger log = LoggerFactory.getLogger(YandexBotService.class);
	private static final int MAX_TEXT_CHUNK = 5800;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final YandexMessengerClient messengerClient;
	private final CommandHandler commandHandler;
	private final MessageHandler messageHandler;
	private final DocumentHandler documentHandler;
	private final ReportHandler reportHandler;

	private final Map<Integer, Boolean> processedUpdates = new ConcurrentHashMap<>();
	private static final Map<String, FeedbackPair> feedbackMemory = new ConcurrentHashMap<>();
	private static final Map<String, Long> lastCallbackTime = new ConcurrentHashMap<>();
	private static final long CALLBACK_COOLDOWN_MS = 2000L;

	public YandexBotService(YandexMessengerClient messengerClient,
			CommandHandler commandHandler,
			MessageHandler messageHandler,
			DocumentHandler documentHandler,
			ReportHandler reportHandler) {
		this.messengerClient = messengerClient;
		this.commandHandler = commandHandler;
		this.messageHandler = messageHandler;
		this.documentHandler = documentHandler;
		this.reportHandler = reportHandler;
	}

	@Async
	public void handleWebhookPayloadAsync(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode updates = root.path("updates");
			if (updates.isArray()) {
				for (JsonNode u : updates) {
					processUpdate(u);
				}
			} else {
				log.warn("Вебхук без массива updates: {}", body.length() > 500 ? body.substring(0, 500) : body);
			}
		} catch (Exception e) {
			log.error("Ошибка разбора вебхука: {}", e.getMessage(), e);
		}
	}

	public void processUpdate(JsonNode update) {
		int updateId = update.path("update_id").asInt(0);
		if (updateId > 0) {
			if (processedUpdates.size() > 100_000) {
				processedUpdates.clear();
			}
			if (processedUpdates.putIfAbsent(updateId, Boolean.TRUE) != null) {
				log.debug("Пропуск дубликата update_id={}", updateId);
				return;
			}
		}

		YandexChatTarget target = chatTargetFromUpdate(update);
		String chatKey = chatKey(target);
		Long messageId = update.hasNonNull("message_id") ? update.get("message_id").longValue() : null;

		JsonNode callbackPayload = extractCallbackPayload(update);
		if (callbackPayload != null && !callbackPayload.isMissingNode() && callbackPayload.size() > 0) {
			long now = System.currentTimeMillis();
			Long lastCb = lastCallbackTime.get(chatKey);
			if (lastCb != null && (now - lastCb) < CALLBACK_COOLDOWN_MS) {
				return;
			}
			lastCallbackTime.put(chatKey, now);
			if (handleActionJson(target, messageId, callbackPayload, chatKey)) {
				return;
			}
		}

		JsonNode fileNode = update.path("file");
		if (!fileNode.isMissingNode() && fileNode.hasNonNull("id")) {
			String fid = fileNode.get("id").asText();
			String name = fileNode.path("name").asText("file");
			int size = fileNode.path("size").asInt(0);
			String reply = documentHandler.processDocument(fid, name, size, chatKey);
			sendPlain(target, messageId, reply);
			return;
		}

		if (update.hasNonNull("images") || update.hasNonNull("sticker")) {
			sendPlain(target, messageId, "Получены изображения или стикер. Обработка медиа в этом боте не реализована — опишите задачу текстом.");
			return;
		}

		String text = update.path("text").asText("").trim();
		if (text.isEmpty()) {
			log.debug("Пустое обновление без текста и файла, update_id={}", updateId);
			return;
		}

		if (text.startsWith("{") && text.contains("\"action\"")) {
			try {
				JsonNode synthetic = objectMapper.readTree(text);
				if (handleActionJson(target, messageId, synthetic, chatKey)) {
					return;
				}
			} catch (Exception ignored) {
			}
		}

		/* Текстовые «колбэки» в стиле Telegram (если платформа присылает строку) */
		if (handleLegacyActionString(target, messageId, text, chatKey)) {
			return;
		}

		if (commandHandler.isCommand(text)) {
			String response = commandHandler.processCommand(text, chatKey);
			if ("/start".equals(text)) {
				sendTextWithKeyboard(target, messageId, response, commandHandler.getMainMenuKeyboard());
			} else {
				sendPlain(target, messageId, response);
			}
			return;
		}

		String response = messageHandler.processMessage(text, chatKey);
		sendWithFeedback(target, messageId, text, response);
	}

	private boolean handleLegacyActionString(YandexChatTarget target, Long messageId, String text, String chatKey) {
		return switch (text) {
			case "chat_with_assistant" -> {
				sendPlain(target, messageId, "💬 Режим чата с ассистентом активирован!\n\n"
						+ "Задавайте вопросы о ремонте оборудования.");
				yield true;
			}
			case "request_report" -> {
				sendTextWithKeyboard(target, messageId, reportHandler.getReportMenuMessage(), reportHandler.getReportMenuKeyboard());
				yield true;
			}
			case "daily_report" -> {
				String report = reportHandler.generateDailyReport();
				sendTextWithKeyboard(target, messageId, report, reportHandler.getBackToReportsKeyboard());
				yield true;
			}
			case "current_report" -> {
				String report = reportHandler.generateCurrentReport();
				sendTextWithKeyboard(target, messageId, report, reportHandler.getBackToReportsKeyboard());
				yield true;
			}
			case "back_to_main" -> {
				sendTextWithKeyboard(target, messageId, commandHandler.handleStart(chatKey), commandHandler.getMainMenuKeyboard());
				yield true;
			}
			default -> false;
		};
	}

	private boolean handleActionJson(YandexChatTarget target, Long messageId, JsonNode cb, String chatKey) {
		String action = cb.path("action").asText("");
		String id = cb.path("id").asText("");

		if ("feedback_correct".equals(action) && !id.isEmpty()) {
			FeedbackPair pair = feedbackMemory.get(id);
			if (pair != null) {
				try {
					messageHandler.getCoreServiceClient().saveFeedback(pair.userQuery, pair.answer);
					sendPlain(target, messageId, "Спасибо! Ответ сохранён для дообучения модели.");
				} catch (Exception e) {
					sendPlain(target, messageId, "Ошибка при сохранении обратной связи: " + e.getMessage());
				}
			} else {
				sendPlain(target, messageId, "Не удалось найти исходный запрос для этой кнопки.");
			}
			return true;
		}
		if ("feedback_retry".equals(action) && !id.isEmpty()) {
			FeedbackPair pair = feedbackMemory.get(id);
			if (pair != null) {
				String newAnswer = messageHandler.processMessage(pair.userQuery, chatKey);
				sendWithFeedback(target, messageId, pair.userQuery, newAnswer);
			} else {
				sendPlain(target, messageId, "Не удалось найти исходный запрос.");
			}
			return true;
		}
		if ("feedback_new".equals(action)) {
			sendPlain(target, messageId, "Диалог сброшен. Можете задать новый вопрос.");
			return true;
		}

		if (action.isEmpty() && cb.has("data")) {
			action = cb.get("data").asText("");
		}

		if (handleLegacyActionString(target, messageId, action, chatKey)) {
			return true;
		}
		sendPlain(target, messageId, "Действие не распознано.");
		return true;
	}

	private void sendWithFeedback(YandexChatTarget target, Long replyToId, String userQuery, String answer) {
		String feedbackId = shortHash(userQuery + "::" + answer);
		feedbackMemory.put(feedbackId, new FeedbackPair(userQuery, answer));

		Map<String, Object> cbOk = Map.of("action", "feedback_correct", "id", feedbackId);
		Map<String, Object> cbRetry = Map.of("action", "feedback_retry", "id", feedbackId);
		Map<String, Object> cbNew = Map.of("action", "feedback_new");

		List<Map<String, Object>> keyboard = List.of(
				Map.of("text", "✅ Ответ корректен", "callback_data", cbOk),
				Map.of("text", "🔄 Сгенерировать заново", "callback_data", cbRetry),
				Map.of("text", "🆕 Новый диалог", "callback_data", cbNew)
		);

		sendTextChunks(target, replyToId, answer, keyboard);
	}

	private void sendPlain(YandexChatTarget target, Long replyToId, String text) {
		sendTextChunks(target, replyToId, text, null);
	}

	private void sendTextWithKeyboard(YandexChatTarget target, Long replyToId, String text,
			List<Map<String, Object>> inlineKeyboard) {
		sendTextChunks(target, replyToId, text, inlineKeyboard);
	}

	private void sendTextChunks(YandexChatTarget target, Long replyToId, String text,
			List<Map<String, Object>> firstChunkKeyboard) {
		if (text == null) {
			text = "";
		}
		if (text.length() <= MAX_TEXT_CHUNK) {
			messengerClient.sendText(target, text, replyToId, firstChunkKeyboard);
			return;
		}
		int start = 0;
		boolean first = true;
		while (start < text.length()) {
			int end = Math.min(start + MAX_TEXT_CHUNK, text.length());
			String part = text.substring(start, end);
			messengerClient.sendText(target, part, replyToId, first ? firstChunkKeyboard : null);
			first = false;
			start = end;
		}
	}

	private static JsonNode extractCallbackPayload(JsonNode update) {
		if (update.hasNonNull("callback_data")) {
			JsonNode n = update.get("callback_data");
			if (n.isTextual()) {
				try {
					return new ObjectMapper().readTree(n.asText());
				} catch (Exception e) {
					return null;
				}
			}
			return n;
		}
		if (update.hasNonNull("callback")) {
			return update.get("callback");
		}
		return null;
	}

	private static YandexChatTarget chatTargetFromUpdate(JsonNode update) {
		JsonNode chat = update.path("chat");
		String type = chat.path("type").asText("");
		String chatId = chat.hasNonNull("id") ? chat.get("id").asText() : null;
		JsonNode from = update.path("from");
		String login = from.hasNonNull("login") ? from.get("login").asText() : null;
		return new YandexChatTarget(type, chatId, login);
	}

	private static String chatKey(YandexChatTarget t) {
		if (t.needsLogin()) {
			return "p:" + t.login();
		}
		if (t.chatId() != null && !t.chatId().isBlank()) {
			return "g:" + t.chatId();
		}
		return "x:" + System.identityHashCode(t);
	}

	private static String shortHash(String s) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOf(hash, 8));
		} catch (Exception e) {
			return Integer.toHexString(s.hashCode());
		}
	}

	private static final class FeedbackPair {
		final String userQuery;
		final String answer;

		FeedbackPair(String userQuery, String answer) {
			this.userQuery = userQuery;
			this.answer = answer;
		}
	}
}
