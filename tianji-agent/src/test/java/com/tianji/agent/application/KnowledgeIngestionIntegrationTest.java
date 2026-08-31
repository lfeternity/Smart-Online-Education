package com.tianji.agent.application;

import com.tianji.agent.domain.*;
import com.tianji.agent.persistence.KnowledgeChunkRepository;
import com.tianji.agent.persistence.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.agent.api.ConversationDtos.ChatContext;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.tool.AgentRequestContext;
import com.tianji.agent.tool.AgentTools;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "agent.limits.enabled=false",
        "agent.knowledge.max-retries=0",
        "agent.knowledge.retry-poll-delay=1h",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class KnowledgeIngestionIntegrationTest {
    @Autowired KnowledgeService knowledge;
    @Autowired IngestionJobService jobs;
    @Autowired KnowledgeDocumentRepository documents;
    @Autowired KnowledgeChunkRepository chunks;

    @Test
    void asynchronouslyPublishesAndAtomicallySwitchesVersion() {
        KnowledgeDocumentEntity first = knowledge.create(new KnowledgeService.KnowledgeInput(
                100L, 10L, 11L, KnowledgeSourceType.TEACHER_DOCUMENT, "lesson-1",
                "Lesson", "first version content", "ENROLLED", null));
        IngestionJobEntity firstJob = jobs.enqueue(first.getId(), 1L);
        await(() -> jobs.get(firstJob.getId()).getStatus() == IngestionJobStatus.SUCCEEDED);

        KnowledgeDocumentEntity second = knowledge.createNextVersion(first.getId());
        second.setContent("second version content");
        documents.save(second);
        IngestionJobEntity secondJob = jobs.enqueue(second.getId(), 1L);
        await(() -> jobs.get(secondJob.getId()).getStatus() == IngestionJobStatus.SUCCEEDED);

        assertEquals(KnowledgeStatus.ARCHIVED, documents.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(KnowledgeStatus.ACTIVE, documents.findById(second.getId()).orElseThrow().getStatus());
        assertTrue(chunks.findByDocumentIdOrderByChunkIndexAsc(first.getId()).stream()
                .noneMatch(KnowledgeChunkEntity::getActive));
        assertTrue(chunks.findByDocumentIdOrderByChunkIndexAsc(second.getId()).stream()
                .allMatch(KnowledgeChunkEntity::getActive));
    }

    @Test
    void invalidTranscriptMovesToDeadLetter() {
        KnowledgeDocumentEntity document = knowledge.create(new KnowledgeService.KnowledgeInput(
                200L, 20L, 21L, KnowledgeSourceType.TRANSCRIPT, "video-1",
                "Broken transcript", "not-a-timeline", "ENROLLED", null));
        IngestionJobEntity job = jobs.enqueue(document.getId(), 1L);

        await(() -> jobs.get(job.getId()).getStatus() == IngestionJobStatus.DEAD_LETTER);

        IngestionJobEntity failed = jobs.get(job.getId());
        assertEquals(IngestionJobStatus.DEAD_LETTER, failed.getStatus());
        assertNotNull(failed.getErrorCode());
        assertFalse(failed.getErrorMessage().isBlank());
    }

    @Test
    void courseKnowledgeToolReturnsOnlyAuthorizedLocalRagHits() {
        KnowledgeDocumentEntity document = knowledge.create(new KnowledgeService.KnowledgeInput(
                300L, 30L, 31L, KnowledgeSourceType.TEACHER_DOCUMENT, "rag-tool",
                "Java concurrency", "CompletableFuture supports asynchronous composition.", "ENROLLED", null));
        knowledge.publish(document.getId());
        BusinessClients clients = Mockito.mock(BusinessClients.class);
        Mockito.when(clients.hasCourseAccess(7L, "request", 300L)).thenReturn(true);
        AgentTools tools = new AgentTools(new AgentRequestContext(7L, "request", "conversation", "message",
                new ChatContext(300L, 30L, 31L, 0, "learning")), clients,
                Mockito.mock(PendingActionService.class), knowledge, new ObjectMapper());

        var result = tools.retrieveCourseKnowledge("CompletableFuture asynchronous", 300L);

        assertTrue(result.success());
        assertFalse(tools.retrievedCitations().isEmpty());
        assertTrue(tools.retrievedCitations().stream().allMatch(hit -> hit.courseId().equals(300L)));
    }

    private void await(Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) return;
            try { Thread.sleep(50); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); fail("interrupted"); }
        }
        fail("condition was not met before timeout");
    }
}
