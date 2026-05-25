package ru.georgdeveloper.assistantyandexbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistantyandexbot.config.YandexBotProperties;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Персистентный offset для {@code getUpdates}.
 * Без сохранения на диск перезапуск бота сбрасывает offset в 0 и повторно обрабатывает
 * всю накопленную очередь — это приводит к массовой рассылке старых ответов.
 */
@Component
public class YandexPollingOffsetStore {

	private static final Logger log = LoggerFactory.getLogger(YandexPollingOffsetStore.class);

	private final YandexBotProperties properties;
	private final Path offsetFile;
	private volatile int nextOffset;

	public YandexPollingOffsetStore(YandexBotProperties properties) {
		this.properties = properties;
		this.offsetFile = Path.of(properties.getPolling().getOffsetFile()).toAbsolutePath().normalize();
		this.nextOffset = 0;
	}

	@PostConstruct
	void loadOnStartup() {
		if (!Files.isRegularFile(offsetFile)) {
			log.warn("Файл offset не найден ({}). При первом getUpdates(offset=0) возможна повторная "
					+ "обработка накопленной очереди. После успешного polling offset будет сохранён.", offsetFile);
			return;
		}
		try {
			String raw = Files.readString(offsetFile, StandardCharsets.UTF_8).trim();
			int loaded = Integer.parseInt(raw);
			if (loaded < 0) {
				log.warn("Некорректный offset в файле {}: {}", offsetFile, raw);
				return;
			}
			nextOffset = loaded;
			log.info("Загружен polling offset={} из {}", nextOffset, offsetFile);
		} catch (Exception e) {
			log.error("Не удалось прочитать offset из {}: {}", offsetFile, e.getMessage(), e);
		}
	}

	public int getNextOffset() {
		return nextOffset;
	}

	public void advanceTo(int updateId) {
		if (updateId <= 0) {
			return;
		}
		int newOffset = updateId + 1;
		if (newOffset <= nextOffset) {
			return;
		}
		nextOffset = newOffset;
		persist(newOffset);
	}

	private void persist(int offset) {
		try {
			Path parent = offsetFile.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tmp = offsetFile.resolveSibling(offsetFile.getFileName() + ".tmp");
			Files.writeString(tmp, Integer.toString(offset), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.move(tmp, offsetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			log.error("Не удалось сохранить polling offset {} в {}: {}", offset, offsetFile, e.getMessage(), e);
		}
	}
}
