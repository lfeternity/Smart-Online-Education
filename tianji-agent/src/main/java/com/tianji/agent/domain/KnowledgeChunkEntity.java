package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_knowledge_chunk", indexes = {
        @Index(name = "idx_chunk_document", columnList = "documentId"),
        @Index(name = "idx_chunk_course_active", columnList = "courseId,active")
})
public class KnowledgeChunkEntity extends AuditedEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String documentId;

    private Long courseId;
    private Long chapterId;
    private Long sectionId;

    @Column(nullable = false, length = 32)
    private String sourceType;

    @Column(length = 80)
    private String sourceId;

    @Column(nullable = false, length = 300)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 64)
    private String contentHash;

    private Integer startMoment;
    private Integer endMoment;
    private Integer chunkIndex;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(length = 80)
    private String embeddingModel;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
