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
 * - Chat: 默认 OpenAI auto-config (DeepSeek)
 * - Embedding: 硅基流动 SiliconFlow (独立 OpenAiApi)
 */
@Configuration
public class AIConfig {

    @Value("${siliconflow.api-key}")
    private String siliconflowApiKey;

    @Value("${siliconflow.base-url}")
    private String siliconflowBaseUrl;

    @Value("${siliconflow.embedding-model}")
    private String siliconflowEmbeddingModel;

    @Bean
    @ConditionalOnMissingBean
    public EmbeddingModel embeddingModel() {
        OpenAiApi siliconflowApi = new OpenAiApi(siliconflowBaseUrl, siliconflowApiKey);
        var options = org.springframework.ai.openai.OpenAiEmbeddingOptions.builder()
                .withModel(siliconflowEmbeddingModel)
                .build();
        return new OpenAiEmbeddingModel(siliconflowApi,
                org.springframework.ai.document.MetadataMode.EMBED, options);
    }
}
