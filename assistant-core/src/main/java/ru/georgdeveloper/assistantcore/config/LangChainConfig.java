package ru.georgdeveloper.assistantcore.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Конфигурация LangChain4j для AI ассистента
 */
@Configuration
public class LangChainConfig {

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ai.ollama.chat-model:deepseek-r1:latest}")
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
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(chatModel)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * Модель эмбеддингов для векторизации текста
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Векторное хранилище ChromaDB
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(collectionName)
                .build();
    }
}
