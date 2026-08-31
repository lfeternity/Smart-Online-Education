package com.tianji.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AiModelConfigurationTest {
    private MockWebServer server;

    @AfterEach
    void close() throws IOException {
        if (server != null) server.close();
    }

    @Test
    void localHashProviderDoesNotRequireAnEmbeddingEndpoint() {
        AgentProperties properties = new AgentProperties();
        properties.getAi().setEmbeddingProvider(AgentProperties.Ai.EmbeddingProvider.LOCAL_HASH);
        properties.getQdrant().setDimension(96);

        EmbeddingModel model = new AiModelConfiguration().embeddingModel(properties);

        assertInstanceOf(LocalHashEmbeddingModel.class, model);
        assertEquals(96, model.dimension());
    }

    @Test
    void chatCompletionsUsesExpectedEndpointAndPayload() throws Exception {
        RecordedRequest request = invoke(AgentProperties.Ai.Protocol.CHAT_COMPLETIONS);
        assertEquals("/v1/chat/completions", request.getPath());
        JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
        assertTrue(body.path("messages").isArray());
        assertTrue(body.path("stream").asBoolean());
    }

    @Test
    void responsesUsesExpectedEndpointAndOmitsUnsupportedParameters() throws Exception {
        RecordedRequest request = invoke(AgentProperties.Ai.Protocol.RESPONSES);
        assertEquals("/v1/responses", request.getPath());
        JsonNode body = new ObjectMapper().readTree(request.getBody().readUtf8());
        assertTrue(body.path("input").isArray());
        assertFalse(body.has("temperature"));
        assertFalse(body.has("max_tool_calls"));
    }

    private RecordedRequest invoke(AgentProperties.Ai.Protocol protocol) throws Exception {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody("upstream unavailable"));
        server.start();
        AgentProperties properties = new AgentProperties();
        properties.getAi().setEnabled(true);
        properties.getAi().setApiKey("test-key");
        properties.getAi().setProtocol(protocol);
        properties.getAi().setBaseUrl(server.url("/v1").toString());
        properties.getAi().setChatModel("test-model");
        StreamingChatModel model = new AiModelConfiguration().streamingChatModel(properties);
        CountDownLatch terminal = new CountDownLatch(1);
        model.chat(ChatRequest.builder().messages(UserMessage.from("hello")).build(), handler(terminal));
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(terminal.await(5, TimeUnit.SECONDS));
        return request;
    }

    private StreamingChatResponseHandler handler(CountDownLatch terminal) {
        return new StreamingChatResponseHandler() {
            @Override public void onCompleteResponse(ChatResponse response) { terminal.countDown(); }
            @Override public void onError(Throwable error) { terminal.countDown(); }
        };
    }
}
