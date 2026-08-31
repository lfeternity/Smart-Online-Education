package com.tianji.agent.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiResponsesStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "agent.ai", name = "enabled", havingValue = "true")
    StreamingChatModel streamingChatModel(AgentProperties properties) {
        AgentProperties.Ai ai = properties.getAi();
        if (ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            throw new IllegalStateException("AGENT_AI_API_KEY is required when agent.ai.enabled=true");
        }
        StreamingChatModel primary = model(ai.getProtocol(), ai.getBaseUrl(), ai.getApiKey(), ai.getChatModel(),
                ai.getTemperature(), ai.getTimeout());
        AgentProperties.Ai.Fallback fallback = ai.getFallback();
        if (!fallback.isEnabled()) return primary;
        if (fallback.getApiKey() == null || fallback.getApiKey().isBlank()
                || fallback.getBaseUrl() == null || fallback.getBaseUrl().isBlank()
                || fallback.getChatModel() == null || fallback.getChatModel().isBlank()) {
            throw new IllegalStateException("Fallback API key, base URL and model are required when fallback is enabled");
        }
        StreamingChatModel secondary = model(fallback.getProtocol(), fallback.getBaseUrl(), fallback.getApiKey(),
                fallback.getChatModel(), fallback.getTemperature(), ai.getTimeout());
        return new FailoverStreamingChatModel(primary, secondary, ai.getCircuit().getFailureThreshold(),
                ai.getCircuit().getOpenDuration());
    }

    private StreamingChatModel model(AgentProperties.Ai.Protocol protocol, String baseUrl, String apiKey,
                                     String model, Double temperature, java.time.Duration timeout) {
        var httpClient = new CancellableHttpClientBuilder();
        httpClient.connectTimeout(timeout).readTimeout(timeout);
        if (protocol == AgentProperties.Ai.Protocol.RESPONSES) {
            return OpenAiResponsesStreamingChatModel.builder()
                    .httpClientBuilder(httpClient).baseUrl(baseUrl).apiKey(apiKey).modelName(model).parallelToolCalls(false)
                    .logRequests(false).logResponses(false).build();
        }
        return OpenAiStreamingChatModel.builder()
                .httpClientBuilder(httpClient).baseUrl(baseUrl).apiKey(apiKey).modelName(model).temperature(temperature).timeout(timeout)
                .parallelToolCalls(false).logRequests(false).logResponses(false).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.ai", name = "enabled", havingValue = "true")
    EmbeddingModel embeddingModel(AgentProperties properties) {
        AgentProperties.Ai ai = properties.getAi();
        if (ai.getEmbeddingProvider() == AgentProperties.Ai.EmbeddingProvider.LOCAL_HASH) {
            return new LocalHashEmbeddingModel(properties.getQdrant().getDimension());
        }
        String baseUrl = ai.getEmbeddingBaseUrl() == null || ai.getEmbeddingBaseUrl().isBlank()
                ? ai.getBaseUrl() : ai.getEmbeddingBaseUrl();
        String apiKey = ai.getEmbeddingApiKey() == null || ai.getEmbeddingApiKey().isBlank()
                ? ai.getApiKey() : ai.getEmbeddingApiKey();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl).apiKey(apiKey).modelName(ai.getEmbeddingModel())
                .dimensions(properties.getQdrant().getDimension()).timeout(ai.getTimeout()).maxRetries(2)
                .logRequests(false).logResponses(false).build();
    }
}
