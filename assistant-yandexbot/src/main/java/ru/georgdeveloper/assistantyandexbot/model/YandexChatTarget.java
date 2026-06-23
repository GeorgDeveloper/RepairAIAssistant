package ru.georgdeveloper.assistantyandexbot.model;

/**
 * Куда отправлять исходящее сообщение Bot API: в группу/канал ({@code chatId}) или в личку ({@code login}).
 */
public record YandexChatTarget(String chatType, String chatId, String login) {

	public boolean isPrivate() {
		return "private".equalsIgnoreCase(chatType);
	}

	public boolean needsLogin() {
		return isPrivate() && login != null && !login.isBlank();
	}
}
