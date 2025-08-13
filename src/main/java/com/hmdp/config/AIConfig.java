package com.hmdp.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AI相关配置类
 * 用于配置AI服务相关的Bean和参数
 */
@Configuration
public class AIConfig {

    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public ChatMemory chatMemory() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        return memory;
    }

    @Bean
    public ChatMemoryProvider redisChatMemoryStore() {
        ChatMemoryProvider memory = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .id(memoryId)
                        .build();
            }
        };
        return memory;
    }

    @Bean
    public EmbeddingStore store() {
        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel).build();
        ingestor.ingest(documents);
        return store;
    }

}
