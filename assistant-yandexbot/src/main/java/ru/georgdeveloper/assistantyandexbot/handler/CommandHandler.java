package ru.georgdeveloper.assistantyandexbot.handler;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandHandler {

	public String handleStart(String chatKey) {
		return "🔧 Добро пожаловать в Repair AI Assistant!\n\n"
				+ "Я помогу вам с вопросами по ремонту оборудования и предоставлю отчеты по производственным показателям.\n\n"
				+ "Выберите действие:";
	}

	public List<Map<String, Object>> getMainMenuKeyboard() {
		return Arrays.asList(
				inlineButton("💬 Чат с ассистентом", callbackAction("chat_with_assistant")),
				inlineButton("📊 Запрос отчета", callbackAction("request_report"))
		);
	}

	public String handleHelp(String chatKey) {
		return "📋 Доступные команды:\n\n"
				+ "/start - Начать работу с ботом\n"
				+ "/help - Показать эту справку\n\n"
				+ "💡 Примеры запросов:\n"
				+ "• Найди самые продолжительные ремонты\n"
				+ "• Как починить телевизор\n"
				+ "• Проблемы с холодильником";
	}

	public boolean isCommand(String message) {
		return message != null && message.startsWith("/");
	}

	public String processCommand(String command, String chatKey) {
		return switch (command) {
			case "/start" -> handleStart(chatKey);
			case "/help" -> handleHelp(chatKey);
			default -> "Неизвестная команда. Используйте /help для списка команд.";
		};
	}

	private static Map<String, Object> callbackAction(String action) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("action", action);
		return data;
	}

	private static Map<String, Object> inlineButton(String text, Map<String, Object> callbackData) {
		Map<String, Object> button = new LinkedHashMap<>();
		button.put("text", text);
		button.put("callback_data", callbackData);
		return button;
	}
}
