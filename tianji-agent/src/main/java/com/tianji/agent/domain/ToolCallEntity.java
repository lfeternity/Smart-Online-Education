package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_tool_call", indexes = @Index(name = "idx_tool_message", columnList = "messageId"))
public class ToolCallEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String messageId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String toolName;

    @Column(nullable = false, length = 64)
    private String argumentsDigest;

    @Column(nullable = false, length = 16)
    private String status;

    private Long latencyMs;

    @Column(length = 64)
    private String errorCode;
}
