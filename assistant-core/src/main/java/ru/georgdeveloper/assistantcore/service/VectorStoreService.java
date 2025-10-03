package ru.georgdeveloper.assistantcore.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.georgdeveloper.assistantcore.model.EquipmentMaintenanceRecord;
import ru.georgdeveloper.assistantcore.model.SummaryOfSolutions;
import ru.georgdeveloper.assistantcore.model.BreakdownReport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с векторным хранилищем ChromaDB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${ai.chroma.url:http://localhost:8000}")
    private String chromaUrl;
    
    @Value("${ai.chroma.collection-name:repair_knowledge}")
    private String collectionName;

    /**
     * Добавляет запись обслуживания оборудования в векторное хранилище
     */
    public void addMaintenanceRecord(EquipmentMaintenanceRecord record) {
        try {
            String content = buildMaintenanceContent(record);
            Metadata metadata = Metadata.from("type", "maintenance")
                    .put("id", record.getId().toString())
                    .put("machine_name", safeString(record.getMachineName()))
                    .put("status", safeString(record.getStatus()))
                    .put("failure_type", safeString(record.getFailureType()));

            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            
            embeddingStore.add(embedding, segment);
            log.debug("Добавлена запись обслуживания ID: {}", record.getId());
        } catch (Exception e) {
            log.error("Ошибка добавления записи обслуживания ID: {}", record.getId(), e);
        }
    }

    /**
     * Добавляет решение сложного ремонта в векторное хранилище
     */
    public void addSolutionRecord(SummaryOfSolutions solution) {
        try {
            String content = buildSolutionContent(solution);
            Metadata metadata = Metadata.from("type", "solution")
                    .put("id", solution.getId().toString())
                    .put("equipment", safeString(solution.getEquipment()))
                    .put("node", safeString(solution.getNode()));

            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            
            embeddingStore.add(embedding, segment);
            log.debug("Добавлено решение ID: {}", solution.getId());
        } catch (Exception e) {
            log.error("Ошибка добавления решения ID: {}", solution.getId(), e);
        }
    }

    /**
     * Добавляет отчет о поломке в векторное хранилище
     */
    public void addBreakdownRecord(BreakdownReport breakdown) {
        try {
            String content = buildBreakdownContent(breakdown);
            Metadata metadata = Metadata.from("type", "breakdown")
                    .put("id", safeString(breakdown.getIdCode()))
                    .put("machine_name", safeString(breakdown.getMachineName()))
                    .put("assembly", safeString(breakdown.getAssembly()))
                    .put("status", safeString(breakdown.getWoStatusLocalDescr()));

            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            
            embeddingStore.add(embedding, segment);
            log.debug("Добавлен отчет о поломке ID: {}", breakdown.getIdCode());
        } catch (Exception e) {
            log.error("Ошибка добавления отчета о поломке ID: {}", breakdown.getIdCode(), e);
        }
    }

    /**
     * Добавляет пользовательский вопрос и ответ в векторное хранилище
     * Используется для сохранения обратной связи от пользователей
     */
    public void addUserFeedback(String userQuery, String assistantResponse) {
        try {
            String content = buildFeedbackContent(userQuery, assistantResponse);
            Metadata metadata = Metadata.from("type", "user_feedback")
                    .put("query", safeString(userQuery))
                    .put("response_preview", safeString(assistantResponse.length() > 100 ? 
                        assistantResponse.substring(0, 100) + "..." : assistantResponse))
                    .put("timestamp", String.valueOf(System.currentTimeMillis()));

            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            
            embeddingStore.add(embedding, segment);
            log.info("Добавлена обратная связь пользователя: {}", userQuery.substring(0, Math.min(50, userQuery.length())));
        } catch (Exception e) {
            log.error("Ошибка добавления обратной связи пользователя: {}", userQuery, e);
        }
    }

    /**
     * Выполняет семантический поиск по запросу
     */
    public List<TextSegment> searchSimilar(String query, int maxResults) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.7) // Минимальная схожесть
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            return searchResult.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка поиска по запросу: {}", query, e);
            return List.of();
        }
    }

    /**
     * Смарт-поиск: семантический + ключевые слова + (опционально) фильтры по метаданным
     * Возвращает topK наиболее релевантных сегментов.
     */
    public List<TextSegment> searchSmart(String query, int topK) {
        return searchSmart(query, topK, Collections.emptyMap());
    }

    public List<TextSegment> searchSmart(String query, int topK, Map<String, String> metadataFilters) {
        try {
            // 1) Базовый семантический поиск с запасом кандидатов
            int candidateK = Math.max(topK * 3, topK + 5);
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(candidateK)
                    .minScore(0.5)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            // 2) Фильтрация по метаданным (если заданы)
            List<EmbeddingMatch<TextSegment>> filtered = searchResult.matches().stream()
                    .filter(match -> metadataFilters == null || metadataFilters.isEmpty() || matchesMetadata(match.embedded().metadata(), metadataFilters))
                    .collect(Collectors.toList());

            // 3) Подсчёт keyword-score (BM25 surrogate / простая эвристика)
            Set<String> queryTokens = tokenize(query);

            List<ScoredSegment> scored = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : filtered) {
                double sim = match.score() != null ? match.score() : 0.0;
                double kw = keywordScore(queryTokens, match.embedded().text());
                double combined = sim * 0.7 + kw * 0.3; // веса можно вынести в конфиг
                scored.add(new ScoredSegment(match.embedded(), combined));
            }

            // 4) Сортировка по комбинированному скору и топ-K
            scored.sort(Comparator.comparingDouble(ScoredSegment::score).reversed());
            return scored.stream().limit(topK).map(ScoredSegment::segment).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Ошибка смарт-поиска по запросу: {}", query, e);
            return List.of();
        }
    }

    private boolean matchesMetadata(Metadata metadata, Map<String, String> filters) {
        for (Map.Entry<String, String> f : filters.entrySet()) {
            String key = f.getKey();
            String expected = f.getValue();
            String actual = metadata.getString(key);
            if (expected == null) continue;
            if (actual == null) return false;
            // Не строгое сравнение: contains (нижний регистр)
            if (!actual.toLowerCase().contains(expected.toLowerCase())) return false;
        }
        return true;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String norm = text.toLowerCase().replaceAll("[^a-zA-Zа-яА-Я0-9]+", " ").trim();
        return Arrays.stream(norm.split(" "))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    private double keywordScore(Set<String> queryTokens, String content) {
        if (content == null || content.isBlank() || queryTokens.isEmpty()) return 0.0;
        Set<String> contentTokens = tokenize(content);
        if (contentTokens.isEmpty()) return 0.0;
        long overlap = queryTokens.stream().filter(contentTokens::contains).count();
        return Math.min(1.0, overlap / 5.0); // нормализация: 5 совпадений ~ 1.0
    }

    private record ScoredSegment(TextSegment segment, double score) {}

    /**
     * Выполняет поиск по типу записи
     */
    public List<TextSegment> searchByType(String query, String type, int maxResults) {
        List<TextSegment> allResults = searchSimilar(query, maxResults * 2);
        
        return allResults.stream()
                .filter(segment -> type.equals(segment.metadata().getString("type")))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Формирует текстовое представление записи обслуживания
     */
    private String buildMaintenanceContent(EquipmentMaintenanceRecord record) {
        StringBuilder content = new StringBuilder();
        content.append("Оборудование: ").append(safeString(record.getMachineName())).append("\n");
        content.append("Узел: ").append(safeString(record.getMechanismNode())).append("\n");
        content.append("Описание проблемы: ").append(safeString(record.getDescription())).append("\n");
        content.append("Тип неисправности: ").append(safeString(record.getFailureType())).append("\n");
        content.append("Причина: ").append(safeString(record.getCause())).append("\n");
        content.append("Комментарии по ремонту: ").append(safeString(record.getComments())).append("\n");
        content.append("Статус: ").append(safeString(record.getStatus())).append("\n");
        content.append("Зона: ").append(safeString(record.getArea())).append("\n");
        content.append("Ремонтники: ").append(safeString(record.getMaintainers())).append("\n");
        
        if (record.getTtr() != null) {
            content.append("Время ремонта: ").append(record.getTtr()).append(" мин\n");
        }
        if (record.getMachineDowntime() != null) {
            content.append("Время простоя: ").append(record.getMachineDowntime()).append(" мин\n");
        }
        
        return content.toString();
    }

    /**
     * Формирует текстовое представление решения
     */
    private String buildSolutionContent(SummaryOfSolutions solution) {
        StringBuilder content = new StringBuilder();
        content.append("Оборудование: ").append(safeString(solution.getEquipment())).append("\n");
        content.append("Узел: ").append(safeString(solution.getNode())).append("\n");
        content.append("Описание работы оборудования: ").append(safeString(solution.getNotes_on_the_operation_of_the_equipment())).append("\n");
        content.append("Принятые меры: ").append(safeString(solution.getMeasures_taken())).append("\n");
        content.append("Комментарии: ").append(safeString(solution.getComments())).append("\n");
        
        return content.toString();
    }

    /**
     * Формирует текстовое представление отчета о поломке
     */
    private String buildBreakdownContent(BreakdownReport breakdown) {
        StringBuilder content = new StringBuilder();
        content.append("Машина: ").append(safeString(breakdown.getMachineName())).append("\n");
        content.append("Узел: ").append(safeString(breakdown.getAssembly())).append("\n");
        content.append("Комментарий: ").append(safeString(breakdown.getComment())).append("\n");
        content.append("Статус: ").append(safeString(breakdown.getWoStatusLocalDescr())).append("\n");
        
        if (breakdown.getDuration() != null) {
            content.append("Длительность: ").append(breakdown.getDuration()).append(" мин\n");
        }
        
        return content.toString();
    }

    /**
     * Формирует текстовое представление обратной связи пользователя
     */
    private String buildFeedbackContent(String userQuery, String assistantResponse) {
        StringBuilder content = new StringBuilder();
        content.append("Вопрос пользователя: ").append(safeString(userQuery)).append("\n\n");
        content.append("Ответ ассистента: ").append(safeString(assistantResponse)).append("\n\n");
        content.append("Статус: Подтверждено пользователем\n");
        content.append("Дата: ").append(new java.util.Date()).append("\n");
        
        return content.toString();
    }

    /**
     * Очищает векторное хранилище (для переиндексации)
     */
    public void clearStore() {
        try {
            log.info("Очистка векторного хранилища ChromaDB");
            
            // Удаляем коллекцию через ChromaDB API
            String deleteUrl = chromaUrl + "/api/v1/collections/" + collectionName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            try {
                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, String.class);
                log.info("Коллекция '{}' успешно удалена", collectionName);
            } catch (Exception e) {
                // Коллекция может не существовать - это нормально
                log.debug("Коллекция '{}' не найдена или уже удалена: {}", collectionName, e.getMessage());
            }
            
            log.info("Векторное хранилище очищено и готово к переиндексации");
        } catch (Exception e) {
            log.error("Ошибка очистки векторного хранилища", e);
        }
    }

    /**
     * Безопасное преобразование строки (null -> пустая строка)
     */
    private String safeString(String value) {
        return value != null ? value : "";
    }
}
