package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_knowledge_document", indexes = {
        @Index(name = "idx_knowledge_course_status", columnList = "courseId,status")
})
public class KnowledgeDocumentEntity extends AuditedEntity {

    @Id
    @Column(length = 36)
    private String id;

    private Long courseId;
    private Long chapterId;
    private Long sectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KnowledgeSourceType sourceType;

    @Column(length = 80)
    private String sourceId;

    @Column(nullable = false, length = 300)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KnowledgeStatus status = KnowledgeStatus.DRAFT;

    @Column(nullable = false, length = 16)
    private String visibility = "ENROLLED";

    @Column(length = 500)
    private String sourceUrl;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
