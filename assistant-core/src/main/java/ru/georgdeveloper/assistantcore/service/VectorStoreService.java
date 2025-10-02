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

import java.util.List;
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
                    .put("machine_name", record.getMachineName())
                    .put("status", record.getStatus())
                    .put("failure_type", record.getFailureType());

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
                    .put("equipment", solution.getEquipment())
                    .put("node", solution.getNode());

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
                    .put("id", breakdown.getIdCode())
                    .put("machine_name", breakdown.getMachineName())
                    .put("assembly", breakdown.getAssembly())
                    .put("status", breakdown.getWoStatusLocalDescr());

            TextSegment segment = TextSegment.from(content, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();
            
            embeddingStore.add(embedding, segment);
            log.debug("Добавлен отчет о поломке ID: {}", breakdown.getIdCode());
        } catch (Exception e) {
            log.error("Ошибка добавления отчета о поломке ID: {}", breakdown.getIdCode(), e);
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
        content.append("Оборудование: ").append(record.getMachineName()).append("\n");
        content.append("Узел: ").append(record.getMechanismNode()).append("\n");
        content.append("Описание проблемы: ").append(record.getDescription()).append("\n");
        content.append("Тип неисправности: ").append(record.getFailureType()).append("\n");
        content.append("Причина: ").append(record.getCause()).append("\n");
        content.append("Комментарии по ремонту: ").append(record.getComments()).append("\n");
        content.append("Статус: ").append(record.getStatus()).append("\n");
        content.append("Зона: ").append(record.getArea()).append("\n");
        content.append("Ремонтники: ").append(record.getMaintainers()).append("\n");
        
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
        content.append("Оборудование: ").append(solution.getEquipment()).append("\n");
        content.append("Узел: ").append(solution.getNode()).append("\n");
        content.append("Описание работы оборудования: ").append(solution.getNotes_on_the_operation_of_the_equipment()).append("\n");
        content.append("Принятые меры: ").append(solution.getMeasures_taken()).append("\n");
        content.append("Комментарии: ").append(solution.getComments()).append("\n");
        
        return content.toString();
    }

    /**
     * Формирует текстовое представление отчета о поломке
     */
    private String buildBreakdownContent(BreakdownReport breakdown) {
        StringBuilder content = new StringBuilder();
        content.append("Машина: ").append(breakdown.getMachineName()).append("\n");
        content.append("Узел: ").append(breakdown.getAssembly()).append("\n");
        content.append("Комментарий: ").append(breakdown.getComment()).append("\n");
        content.append("Статус: ").append(breakdown.getWoStatusLocalDescr()).append("\n");
        
        if (breakdown.getDuration() != null) {
            content.append("Длительность: ").append(breakdown.getDuration()).append(" мин\n");
        }
        
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
}
