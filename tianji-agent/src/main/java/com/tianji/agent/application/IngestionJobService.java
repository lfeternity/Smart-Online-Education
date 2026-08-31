package com.tianji.agent.application;

import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.IngestionJobEntity;
import com.tianji.agent.domain.IngestionJobStatus;
import com.tianji.agent.domain.IngestionStage;
import com.tianji.agent.persistence.IngestionJobRepository;
import com.tianji.agent.persistence.KnowledgeDocumentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionJobService {
    private final IngestionJobRepository jobs;
    private final KnowledgeDocumentRepository documents;
    private final AgentProperties properties;
    private final ApplicationEventPublisher events;

    public IngestionJobService(IngestionJobRepository jobs, KnowledgeDocumentRepository documents,
                               AgentProperties properties, ApplicationEventPublisher events) {
        this.jobs = jobs;
        this.documents = documents;
        this.properties = properties;
        this.events = events;
    }

    @Transactional
    public IngestionJobEntity enqueue(String documentId, Long userId) {
        if (!documents.existsById(documentId)) throw AgentException.notFound("知识文档不存在");
        IngestionJobEntity job = new IngestionJobEntity();
        job.setDocumentId(documentId);
        job.setRequestedBy(userId);
        job.setMaxRetries(properties.getKnowledge().getMaxRetries());
        IngestionJobEntity saved = jobs.save(job);
        events.publishEvent(new IngestionRequested(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public IngestionJobEntity get(String id) {
        return jobs.findById(id).orElseThrow(() -> AgentException.notFound("知识摄取任务不存在"));
    }

    @Transactional
    public IngestionJobEntity retry(String id) {
        IngestionJobEntity job = jobs.findById(id).orElseThrow(() -> AgentException.notFound("知识摄取任务不存在"));
        if (job.getStatus() != IngestionJobStatus.DEAD_LETTER) {
            throw AgentException.conflict("只有死信任务可以人工重试");
        }
        job.setStatus(IngestionJobStatus.PENDING);
        job.setStage(IngestionStage.QUEUED);
        job.setRetryCount(0);
        job.setCompletedTime(null);
        job.setNextAttemptTime(null);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        IngestionJobEntity saved = jobs.save(job);
        events.publishEvent(new IngestionRequested(saved.getId()));
        return saved;
    }
}
