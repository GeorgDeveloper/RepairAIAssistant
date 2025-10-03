package ru.georgdeveloper.assistantcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Сервис для улучшения и нормализации пользовательских запросов
 * Повышает качество семантического поиска и классификации
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = false)
public class QueryEnhancementService {

    // Паттерны для очистки запросов
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^\\p{L}\\p{N}\\s\\-.,!?]");
    
    /**
     * Нормализует и улучшает пользовательский запрос
     */
    public String enhanceQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return "";
        }
        
        try {
            String enhanced = originalQuery.trim();
            
            // Удаляем лишние пробелы
            enhanced = EXTRA_SPACES.matcher(enhanced).replaceAll(" ");
            
            // Удаляем специальные символы (кроме базовых знаков препинания)
            enhanced = SPECIAL_CHARS.matcher(enhanced).replaceAll(" ");
            
            // Нормализуем регистр
            enhanced = enhanced.toLowerCase(Locale.ROOT);
            
            // Расширяем сокращения
            enhanced = expandAbbreviations(enhanced);
            
            // Добавляем синонимы для лучшего поиска
            enhanced = addSynonyms(enhanced);
            
            log.debug("Запрос улучшен: '{}' -> '{}'", originalQuery, enhanced);
            return enhanced.trim();
            
        } catch (Exception e) {
            log.warn("Ошибка улучшения запроса '{}': {}", originalQuery, e.getMessage());
            return originalQuery.trim();
        }
    }
    
    /**
     * Расширяет сокращения в запросе
     */
    private String expandAbbreviations(String query) {
        return query
            .replace("не раб", "не работает")
            .replace("сломан", "сломался поломка")
            .replace("течет", "утечка течь")
            .replace("шумит", "шум вибрация")
            .replace("греется", "перегрев нагрев")
            .replace("стучит", "стук вибрация шум")
            .replace("тормозит", "медленно работает")
            .replace("зависает", "останавливается блокируется");
    }
    
    /**
     * Добавляет синонимы для улучшения семантического поиска
     */
    private String addSynonyms(String query) {
        StringBuilder enhanced = new StringBuilder(query);
        
        // Синонимы для оборудования
        if (query.contains("насос")) {
            enhanced.append(" помпа агрегат");
        }
        if (query.contains("двигатель")) {
            enhanced.append(" мотор привод");
        }
        if (query.contains("конвейер")) {
            enhanced.append(" транспортер лента");
        }
        if (query.contains("пресс")) {
            enhanced.append(" прессование сжатие");
        }
        
        // Синонимы для проблем
        if (query.contains("поломка")) {
            enhanced.append(" неисправность авария сбой");
        }
        if (query.contains("ремонт")) {
            enhanced.append(" восстановление починка обслуживание");
        }
        if (query.contains("утечка")) {
            enhanced.append(" течь протечка");
        }
        
        return enhanced.toString();
    }
    
    /**
     * Определяет приоритет запроса для лучшей обработки
     */
    public QueryPriority determineQueryPriority(String query) {
        if (query == null || query.trim().isEmpty()) {
            return QueryPriority.LOW;
        }
        
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        
        // Высокий приоритет - критические проблемы
        if (lowerQuery.contains("авария") || 
            lowerQuery.contains("остановка") ||
            lowerQuery.contains("не работает") ||
            lowerQuery.contains("сломался")) {
            return QueryPriority.HIGH;
        }
        
        // Средний приоритет - проблемы требующие внимания
        if (lowerQuery.contains("утечка") ||
            lowerQuery.contains("шум") ||
            lowerQuery.contains("вибрация") ||
            lowerQuery.contains("перегрев")) {
            return QueryPriority.MEDIUM;
        }
        
        // Низкий приоритет - общие вопросы
        return QueryPriority.LOW;
    }
    
    /**
     * Приоритеты запросов
     */
    public enum QueryPriority {
        HIGH,    // Критические проблемы
        MEDIUM,  // Проблемы требующие внимания  
        LOW      // Общие вопросы
    }
}