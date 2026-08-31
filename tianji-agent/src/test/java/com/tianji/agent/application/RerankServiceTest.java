package com.tianji.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.agent.config.AgentProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RerankServiceTest {
    @Test
    void appliesProviderScoresAndOrder() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("{\"results\":[{\"index\":1,\"relevance_score\":0.91},"
                            + "{\"index\":0,\"relevance_score\":0.42}]}"));
            server.start();
            AgentProperties properties = new AgentProperties();
            properties.getRerank().setEnabled(true);
            properties.getRerank().setBaseUrl(server.url("/").toString());
            RerankService service = new RerankService(properties, new ObjectMapper(), WebClient.builder());

            List<KnowledgeService.SearchHit> result = service.rerank("question",
                    List.of(hit("a", 0.8), hit("b", 0.7)), 2);

            assertEquals("b", result.get(0).chunkId());
            assertEquals(0.91, result.get(0).score(), 0.0001);
            assertEquals("/rerank", server.takeRequest().getPath());
        }
    }

    private KnowledgeService.SearchHit hit(String id, double score) {
        return new KnowledgeService.SearchHit(id, "DOCUMENT", id, 1L, 2L, 3L,
                null, null, id, "content", score);
    }
}
