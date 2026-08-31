package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_ingestion_job", indexes = {
        @Index(name = "idx_ingestion_status_retry", columnList = "status,nextAttemptTime"),
        @Index(name = "idx_ingestion_document", columnList = "documentId,createTime")
})
public class IngestionJobEntity extends AuditedEntity {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String documentId;
    @Column(nullable = false)
    private Long requestedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionJobStatus status = IngestionJobStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionStage stage = IngestionStage.QUEUED;
    @Column(nullable = false)
    private Integer retryCount = 0;
    @Column(nullable = false)
    private Integer maxRetries = 3;
    private LocalDateTime nextAttemptTime;
    @Column(length = 64)
    private String errorCode;
    @Column(length = 500)
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;

    @PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID().toString();
    }
}
