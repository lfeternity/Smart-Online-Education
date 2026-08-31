package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_message", indexes = {
        @Index(name = "idx_message_conversation_time", columnList = "conversationId,createTime")
})
public class MessageEntity extends AuditedEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String conversationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 80)
    private String model;

    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;

    @Column(length = 32)
    private String finishReason;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
