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
 */
@Component
public class YandexPollingOffsetStore {

	private static final Logger log = LoggerFactory.getLogger(YandexPollingOffsetStore.class);

	private final YandexBotProperties properties;
	private final Path offsetFile;
	private volatile long nextOffset;
	private volatile boolean drainingBacklog;

	public YandexPollingOffsetStore(YandexBotProperties properties) {
		this.properties = properties;
		this.offsetFile = Path.of(properties.getPolling().getOffsetFile()).toAbsolutePath().normalize();
		this.nextOffset = 0L;
	}

	@PostConstruct
	void loadOnStartup() {
		if (Files.isRegularFile(offsetFile)) {
			try {
				String raw = Files.readString(offsetFile, StandardCharsets.UTF_8).trim();
				nextOffset = Long.parseLong(raw);
				log.info("Загружен polling offset={} из {}", nextOffset, offsetFile);
			} catch (Exception e) {
				log.error("Не удалось прочитать offset из {}: {}", offsetFile, e.getMessage(), e);
			}
		} else {
			log.warn("Файл offset не найден ({}), начинаем с offset=0", offsetFile);
		}

		if (properties.getPolling().isDrainBacklogOnStartup()) {
			drainingBacklog = true;
			log.info("Старт: сброс накопленной очереди getUpdates без ответов пользователям "
					+ "(отключите yandex.bot.polling.drain-backlog-on-startup=false, если не нужно)");
		}
	}

	public long getNextOffset() {
		return nextOffset;
	}

	public boolean isDrainingBacklog() {
		return drainingBacklog;
	}

	public void finishBacklogDrain() {
		if (drainingBacklog) {
			drainingBacklog = false;
			log.info("Очередь getUpdates сброшена (offset={}). Обрабатываются только новые сообщения.", nextOffset);
		}
	}

	public void advanceTo(long updateId) {
		if (updateId == 0L) {
			return;
		}
		long newOffset = updateId + 1L;
		if (newOffset <= nextOffset) {
			return;
		}
		nextOffset = newOffset;
		persist(newOffset);
	}

	private void persist(long offset) {
		try {
			Path parent = offsetFile.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tmp = offsetFile.resolveSibling(offsetFile.getFileName() + ".tmp");
			Files.writeString(tmp, Long.toString(offset), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.move(tmp, offsetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			log.error("Не удалось сохранить polling offset {} в {}: {}", offset, offsetFile, e.getMessage(), e);
		}
	}
}
