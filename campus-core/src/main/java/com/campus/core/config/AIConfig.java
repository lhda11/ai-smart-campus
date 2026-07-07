package com.campus.core.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 双 Provider AI 配置：
 * - Chat: 默认 OpenAI auto-config
 * - Embedding: 独立 Embedding Provider (独立 OpenAiApi)
 */
@Configuration
public class AIConfig {

    @Value("${embedding.api-key}")
    private String embeddingApiKey;

    @Value("${embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${embedding.model}")
    private String embeddingModel;

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModel embeddingModel() {
        OpenAiApi embeddingApi = new OpenAiApi(embeddingBaseUrl, embeddingApiKey);
        var options = org.springframework.ai.openai.OpenAiEmbeddingOptions.builder()
                .withModel(embeddingModel)
                .build();
        return new OpenAiEmbeddingModel(embeddingApi,
                org.springframework.ai.document.MetadataMode.EMBED, options);
    }
}
