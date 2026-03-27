package ru.georgdeveloper.assistantyandexbot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistantyandexbot.client.YandexMessengerClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class DocumentHandler {

	private static final Logger logger = LoggerFactory.getLogger(DocumentHandler.class);

	private static final String DOWNLOAD_DIR = "downloads";
	/** Защитный лимит для вложений, чтобы не раздувать память/диск. */
	private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

	private final YandexMessengerClient messengerClient;

	public DocumentHandler(YandexMessengerClient messengerClient) {
		this.messengerClient = messengerClient;
	}

	public String processDocument(String fileId, String fileName, long sizeBytes, String chatKey) {
		try {
			// Быстрая валидация до скачивания.
			if (sizeBytes > MAX_FILE_SIZE) {
				return "Файл слишком большой (макс. 20 МБ).";
			}
			logger.info("Загрузка вложения {} ({}) для {}", fileName, fileId, chatKey);
			byte[] data = messengerClient.downloadFile(fileId);
			if (data == null || data.length == 0) {
				return "Ошибка: не удалось загрузить вложение. Попробуйте ещё раз.";
			}
			Path downloadPath = Paths.get(DOWNLOAD_DIR);
			if (!Files.exists(downloadPath)) {
				Files.createDirectories(downloadPath);
			}
			String ext = getFileExtension(fileName);
			Path localFilePath = downloadPath.resolve(UUID.randomUUID() + ext);
			Files.write(localFilePath, data);

			if (isPdfDocument(fileName)) {
				// Сейчас PDF обрабатываем как подтверждение получения.
				return processPdfDocument(localFilePath.toString(), fileName);
			}
			// Для текстовых форматов читаем содержимое и отправляем выдержку.
			return processOtherDocument(localFilePath.toString(), fileName);
		} catch (Exception e) {
			logger.error("Ошибка обработки документа {}: {}", fileName, e.getMessage(), e);
			return "Ошибка при обработке документа: " + e.getMessage();
		}
	}

	public boolean isPdfDocument(String fileName) {
		return fileName != null && fileName.toLowerCase().endsWith(".pdf");
	}

	private String processPdfDocument(String filePath, String fileName) {
		return "PDF документ '" + fileName + "' получен. "
				+ "Функция анализа PDF находится в разработке. Отправьте текст вопроса текстом.";
	}

	private String processOtherDocument(String filePath, String fileName) {
		try {
			String extension = getFileExtension(fileName);
			if (isTextFile(extension)) {
				String content = Files.readString(Path.of(filePath));
				if (content.trim().isEmpty()) {
					return "Файл '" + fileName + "' пуст или не содержит текста.";
				}
				return "Текстовый документ '" + fileName + "' получен. Содержимое: "
						+ (content.length() > 500 ? content.substring(0, 500) + "..." : content);
			}
			return "Документ '" + fileName + "' получен. Поддерживаются текстовые файлы и PDF. "
					+ "Отправьте вопрос текстом.";
		} catch (IOException e) {
			return "Ошибка при чтении файла: " + e.getMessage();
		} finally {
			// Всегда удаляем временный файл, чтобы не копить мусор на диске.
			try {
				Files.deleteIfExists(Path.of(filePath));
			} catch (IOException ignored) {
			}
		}
	}

	private boolean isTextFile(String extension) {
		if (extension == null) {
			return false;
		}
		String ext = extension.toLowerCase();
		return ext.equals(".txt") || ext.equals(".log") || ext.equals(".md")
				|| ext.equals(".json") || ext.equals(".xml") || ext.equals(".csv");
	}

	private String getFileExtension(String filePath) {
		if (filePath == null || !filePath.contains(".")) {
			return "";
		}
		return filePath.substring(filePath.lastIndexOf("."));
	}
}
