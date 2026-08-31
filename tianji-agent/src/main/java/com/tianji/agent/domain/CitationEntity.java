package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_citation", indexes = @Index(name = "idx_citation_message", columnList = "messageId"))
public class CitationEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String messageId;

    @Column(nullable = false, length = 36)
    private String chunkId;

    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(length = 80)
    private String sourceId;

    private Long courseId;
    private Long chapterId;
    private Long sectionId;
    private Integer startMoment;
    private Integer endMoment;

    @Column(nullable = false, length = 300)
    private String title;

    private Double score;
}
