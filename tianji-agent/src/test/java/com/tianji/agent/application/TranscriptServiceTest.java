package com.tianji.agent.application;

import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.KnowledgeDocumentEntity;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class TranscriptServiceTest {
    @Test
    void callsOpenAiCompatibleAsrAndCreatesTimelineDocument() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("{\"text\":\"hello\",\"segments\":[{\"start\":1.2,\"end\":3.7,\"text\":\"hello\"}]}"));
            server.start();
            AgentProperties properties = new AgentProperties();
            properties.getAsr().setEnabled(true);
            properties.getAsr().setBaseUrl(server.url("/v1").toString());
            properties.getAsr().setApiKey("test-key");
            KnowledgeService knowledge = Mockito.mock(KnowledgeService.class);
            KnowledgeDocumentEntity saved = new KnowledgeDocumentEntity(); saved.setId("document");
            Mockito.when(knowledge.create(any())).thenReturn(saved);
            TranscriptService service = new TranscriptService(knowledge, new TranscriptDocumentCodec(),
                    properties, WebClient.builder());

            KnowledgeDocumentEntity result = service.transcribe(
                    new TranscriptService.TranscriptMetadata(1L, 2L, 3L, "video", "title", "ENROLLED"),
                    "audio.mp3", new byte[]{1, 2, 3});

            assertEquals("document", result.getId());
            ArgumentCaptor<KnowledgeService.KnowledgeInput> input = ArgumentCaptor.forClass(KnowledgeService.KnowledgeInput.class);
            Mockito.verify(knowledge).create(input.capture());
            assertTrue(input.getValue().content().contains("[[1:4]] hello"));
            assertEquals("/v1/audio/transcriptions", server.takeRequest().getPath());
        }
    }
}
