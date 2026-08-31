package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_conversation", indexes = {
        @Index(name = "idx_conversation_user_update", columnList = "userId,updateTime")
})
public class ConversationEntity extends AuditedEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 32)
    private String scene = "LEARNING_ASSISTANT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(nullable = false, length = 32)
    private String promptVersion = "learning-assistant-v1";

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
