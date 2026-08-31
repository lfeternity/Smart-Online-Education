package com.tianji.agent.application;

import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.IngestionJobEntity;
import com.tianji.agent.domain.IngestionJobStatus;
import com.tianji.agent.domain.IngestionStage;
import com.tianji.agent.persistence.IngestionJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;

@Service
public class IngestionJobCoordinator {
    private final IngestionJobRepository repository;
    private final AgentProperties properties;

    public IngestionJobCoordinator(IngestionJobRepository repository, AgentProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public IngestionJobEntity claim(String id) {
        IngestionJobEntity job = repository.findByIdForUpdate(id)
                .orElseThrow(() -> AgentException.notFound("知识摄取任务不存在"));
        if (job.getStatus() != IngestionJobStatus.PENDING && job.getStatus() != IngestionJobStatus.RETRY_WAIT) {
            return null;
        }
        if (job.getNextAttemptTime() != null && job.getNextAttemptTime().isAfter(LocalDateTime.now())) return null;
        job.setStatus(IngestionJobStatus.RUNNING);
        job.setStartedTime(LocalDateTime.now());
        job.setErrorCode(null);
        job.setErrorMessage(null);
        return repository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stage(String id, IngestionStage stage) {
        repository.findById(id).filter(job -> job.getStatus() == IngestionJobStatus.RUNNING).ifPresent(job -> {
            job.setStage(stage);
            repository.save(job);
        });
    }

    @Transactional
    public void complete(String id) {
        IngestionJobEntity job = repository.findByIdForUpdate(id).orElseThrow();
        job.setStage(IngestionStage.COMPLETED);
        job.setStatus(IngestionJobStatus.SUCCEEDED);
        job.setCompletedTime(LocalDateTime.now());
        job.setNextAttemptTime(null);
        repository.save(job);
    }

    @Transactional
    public IngestionJobEntity fail(String id, Throwable error) {
        IngestionJobEntity job = repository.findByIdForUpdate(id).orElseThrow();
        int retry = job.getRetryCount() + 1;
        job.setRetryCount(retry);
        job.setErrorCode(errorCode(error));
        job.setErrorMessage(safeMessage(error));
        if (retry > job.getMaxRetries()) {
            job.setStatus(IngestionJobStatus.DEAD_LETTER);
            job.setCompletedTime(LocalDateTime.now());
            job.setNextAttemptTime(null);
        } else {
            long base = Math.max(1, properties.getKnowledge().getRetryBaseDelay().toSeconds());
            job.setStatus(IngestionJobStatus.RETRY_WAIT);
            job.setNextAttemptTime(LocalDateTime.now().plusSeconds(base * (1L << Math.min(retry - 1, 6))));
        }
        return repository.save(job);
    }

    private String errorCode(Throwable error) {
        String name = error == null ? "UNKNOWN" : error.getClass().getSimpleName();
        return name.length() <= 64 ? name : name.substring(0, 64);
    }

    private String safeMessage(Throwable error) {
        String value = error == null || error.getMessage() == null ? "知识摄取失败" : error.getMessage();
        value = value.replaceAll("(?i)(sk-[a-z0-9_-]{8,})", "[redacted]");
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
