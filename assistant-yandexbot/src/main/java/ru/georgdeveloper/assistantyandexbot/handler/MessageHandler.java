package ru.georgdeveloper.assistantyandexbot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.georgdeveloper.assistantyandexbot.client.CoreServiceClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class MessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final CoreServiceClient coreServiceClient;

	public MessageHandler(CoreServiceClient coreServiceClient) {
		this.coreServiceClient = coreServiceClient;
	}

	public CoreServiceClient getCoreServiceClient() {
		return coreServiceClient;
	}

	public String processMessage(String message, String chatKey) {
		return processMessage(message, chatKey, null);
	}

	public String processMessage(String message, String chatKey, ProgressCallback callback) {
		// Две задачи в отдельном пуле:
		// 1) периодический progress callback (например "печатает");
		// 2) основной долгий запрос к assistant-core.
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<?> progressTask = executor.submit(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					if (callback != null) {
						callback.updateProgress();
					}
					Thread.sleep(4000);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		Future<String> mainTask = executor.submit(() -> coreServiceClient.analyzeRepairRequest(message));
		try {
			// Ограничиваем время, чтобы не зависать бесконечно на внешнем сервисе.
			return mainTask.get(4, TimeUnit.MINUTES);
		} catch (TimeoutException e) {
			logger.warn("Таймаут: помощник не ответил за 4 минуты");
			mainTask.cancel(true);
			return "Помощник не справился с задачей. Попробуйте позже.";
		} catch (Exception e) {
			logger.error("Ошибка обработки: {}", e.getMessage(), e);
			return "Помощник не справился с задачей. Попробуйте позже.";
		} finally {
			// Корректно останавливаем обе задачи независимо от исхода.
			progressTask.cancel(true);
			executor.shutdownNow();
		}
	}

	@FunctionalInterface
	public interface ProgressCallback {
		void updateProgress();
	}
}
