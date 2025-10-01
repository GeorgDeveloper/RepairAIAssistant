package ru.georgdeveloper.assistantbaseupdate.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Тесты для сервиса переноса данных
 */
@SpringBootTest
@TestPropertySource(properties = {
    "data-sync.enabled=false"  // Отключаем автоматический запуск для тестов
})
class DataTransferServiceTest {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring загружается корректно
    }
}
