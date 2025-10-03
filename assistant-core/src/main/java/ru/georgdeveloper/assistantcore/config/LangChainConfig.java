package ru.georgdeveloper.assistantcore.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Конфигурация LangChain4j для AI ассистента
 */
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = false)
public class LangChainConfig {

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ai.ollama.chat-model:phi3:mini}")
    private String chatModel;

    @Value("${ai.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${ai.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Value("${ai.chroma.collection-name:repair_knowledge}")
    private String collectionName;

    /**
     * Модель чата для генерации ответов
     */
    @Bean
    @ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(chatModel)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(120))  // Увеличиваем таймаут до 2 минут
                .build();
    }

    /**
     * Модель эмбеддингов для векторизации текста
     */
    @Bean
    @ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(60))  // Увеличиваем таймаут для эмбеддингов
                .build();
    }

    /**
     * Векторное хранилище ChromaDB
     */
    @Bean
    @ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
    public EmbeddingStore<TextSegment> embeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(collectionName)
                .build();
    }
}
