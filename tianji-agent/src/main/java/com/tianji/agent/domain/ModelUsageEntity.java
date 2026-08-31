package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_model_usage", indexes = @Index(name = "idx_usage_user_time", columnList = "userId,createTime"))
public class ModelUsageEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String scene;

    @Column(nullable = false, length = 80)
    private String model;

    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private Long estimatedCostMicros;

    @Column(nullable = false, length = 32)
    private String priceVersion = "unpriced";
}
