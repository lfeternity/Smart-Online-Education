package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_pending_action", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_action_idempotency", columnNames = "idempotencyKey")
}, indexes = @Index(name = "idx_pending_action_user", columnList = "userId,status,expireTime"))
public class PendingActionEntity extends AuditedEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 36)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PendingActionType actionType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PendingActionStatus status = PendingActionStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expireTime;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(length = 300)
    private String resultMessage;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
