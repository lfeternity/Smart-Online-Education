package com.tianji.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.config.LocalHashEmbeddingModel;
import com.tianji.agent.domain.KnowledgeDocumentEntity;
import com.tianji.agent.domain.KnowledgeStatus;
import com.tianji.agent.persistence.KnowledgeChunkRepository;
import com.tianji.agent.persistence.KnowledgeDocumentRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeServiceTest {

    @Test
    void reciprocalRankFusionDeduplicatesAndBoostsCurrentSection() {
        KnowledgeService service = service();
        var dense = List.of(hit("dense-only", 8L, 0.9), hit("shared", 7L, 0.8));
        var lexical = List.of(hit("shared", 7L, 0.7), hit("section-hit", 8L, 0.6));

        List<KnowledgeService.SearchHit> result = service.reciprocalRankFusion(dense, lexical, 8L, 3);

        assertEquals(3, result.size());
        assertEquals("shared", result.get(0).chunkId());
        assertEquals(1, result.stream().filter(value -> value.chunkId().equals("shared")).count());
        assertTrue(result.stream().filter(value -> value.chunkId().equals("section-hit")).findFirst().orElseThrow().score()
                > 1D / 62D);
    }

    @Test
    void reciprocalRankFusionHonorsCandidateLimit() {
        KnowledgeService service = service();
        List<KnowledgeService.SearchHit> result = service.reciprocalRankFusion(
                List.of(hit("a", 1L, 1), hit("b", 1L, 0.9)),
                List.of(hit("c", 1L, 1), hit("d", 1L, 0.9)), null, 2);

        assertEquals(2, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void enabledQdrantSendsLocalVectorAndReturnsDenseHit() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("""
                    {"result":[{"id":"point-1","score":0.72,"payload":{"chunkId":"vector-hit",
                    "documentId":"doc-1","sourceType":"TEACHER_DOCUMENT","sourceId":"lesson-1",
                    "courseId":100,"sectionId":11,"title":"Java concurrency",
                    "content":"CompletableFuture asynchronous composition"}}],"status":"ok"}
                    """));
            server.start();
            AgentProperties properties = new AgentProperties();
            properties.getQdrant().setEnabled(true);
            properties.getQdrant().setBaseUrl(server.url("/").toString());
            properties.getQdrant().setDimension(64);
            properties.getAi().setRetrievalMinScore(0.05D);
            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId("doc-1");
            KnowledgeDocumentRepository documents = Mockito.mock(KnowledgeDocumentRepository.class);
            Mockito.when(documents.findByCourseIdAndStatus(100L, KnowledgeStatus.ACTIVE)).thenReturn(List.of(document));
            KnowledgeChunkRepository chunks = Mockito.mock(KnowledgeChunkRepository.class);
            Mockito.when(chunks.findTop500ByCourseIdAndActiveTrue(100L)).thenReturn(List.of());
            ObjectProvider<EmbeddingModel> provider = Mockito.mock(ObjectProvider.class);
            Mockito.when(provider.getIfAvailable()).thenReturn(new LocalHashEmbeddingModel(64));
            RerankService rerank = new RerankService(properties, new ObjectMapper(), WebClient.builder());
            KnowledgeService service = new KnowledgeService(documents, chunks, properties, provider, rerank,
                    new TranscriptDocumentCodec(), WebClient.builder(), new ObjectMapper());

            List<KnowledgeService.SearchHit> hits = service.search(1L, "request", 100L, 11L,
                    "CompletableFuture asynchronous");

            assertEquals("vector-hit", hits.get(0).chunkId());
            RecordedRequest request = server.takeRequest(3, TimeUnit.SECONDS);
            assertEquals("/collections/course_knowledge_v1/points/search", request.getPath());
            var body = new ObjectMapper().readTree(request.getBody().readUtf8());
            assertEquals(64, body.path("vector").size());
            assertTrue(body.path("filter").path("must").isArray());
        }
    }

    @SuppressWarnings("unchecked")
    private KnowledgeService service() {
        ObjectProvider<EmbeddingModel> provider = Mockito.mock(ObjectProvider.class);
        return new KnowledgeService(Mockito.mock(KnowledgeDocumentRepository.class),
                Mockito.mock(KnowledgeChunkRepository.class), new AgentProperties(), provider,
                Mockito.mock(RerankService.class), new TranscriptDocumentCodec(), WebClient.builder(), new ObjectMapper());
    }

    private KnowledgeService.SearchHit hit(String id, Long sectionId, double score) {
        return new KnowledgeService.SearchHit(id, "DOCUMENT", id, 1L, 2L, sectionId,
                null, null, id, "content", score);
    }
}
