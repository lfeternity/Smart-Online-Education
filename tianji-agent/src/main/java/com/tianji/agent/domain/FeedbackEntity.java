package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_feedback", uniqueConstraints = {
        @UniqueConstraint(name = "uk_feedback_user_message", columnNames = {"userId", "messageId"})
})
public class FeedbackEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 36)
    private String messageId;

    @Column(nullable = false, length = 16)
    private String rating;

    @Column(length = 64)
    private String reason;

    @Column(length = 500)
    private String comment;
}
