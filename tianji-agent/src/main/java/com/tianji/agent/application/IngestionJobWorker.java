package com.tianji.agent.application;

import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.IngestionJobEntity;
import com.tianji.agent.domain.IngestionJobStatus;
import com.tianji.agent.persistence.IngestionJobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class IngestionJobWorker {
    private final IngestionJobCoordinator coordinator;
    private final IngestionJobRepository jobs;
    private final KnowledgeService knowledge;
    private final AgentProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher events;

    public IngestionJobWorker(IngestionJobCoordinator coordinator, IngestionJobRepository jobs,
                              KnowledgeService knowledge, AgentProperties properties,
                              RabbitTemplate rabbitTemplate, ApplicationEventPublisher events) {
        this.coordinator = coordinator;
        this.jobs = jobs;
        this.knowledge = knowledge;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.events = events;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void requested(IngestionRequested event) {
        execute(event.jobId());
    }

    void execute(String jobId) {
        IngestionJobEntity job = coordinator.claim(jobId);
        if (job == null) return;
        try {
            knowledge.publish(job.getDocumentId(), stage -> coordinator.stage(jobId, stage));
            coordinator.complete(jobId);
        } catch (Throwable error) {
            IngestionJobEntity failed = coordinator.fail(jobId, error);
            if (failed.getStatus() == IngestionJobStatus.DEAD_LETTER && properties.getKnowledge().isMqEnabled()) {
                rabbitTemplate.convertAndSend("agent.knowledge.dlx", "knowledge.failed",
                        Map.of("jobId", failed.getId(), "documentId", failed.getDocumentId(),
                                "errorCode", failed.getErrorCode()));
            }
        }
    }

    @Scheduled(fixedDelayString = "${agent.knowledge.retry-poll-delay:5s}")
    public void dispatchRetries() {
        List<IngestionJobEntity> due = jobs
                .findTop20ByStatusInAndNextAttemptTimeLessThanEqualOrderByCreateTimeAsc(
                        List.of(IngestionJobStatus.RETRY_WAIT), LocalDateTime.now());
        due.forEach(job -> events.publishEvent(new IngestionRequested(job.getId())));
    }
}
