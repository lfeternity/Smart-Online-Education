package com.tianji.agent.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ai_prompt_version", uniqueConstraints = @UniqueConstraint(name = "uk_prompt_key_version", columnNames = {"promptKey", "version"}))
public class PromptVersionEntity extends AuditedEntity {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 64) private String promptKey;
    @Column(nullable = false, length = 32) private String version;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
    @Column(nullable = false, length = 64) private String contentHash;
    @Column(nullable = false, length = 16) private String status = "DRAFT";
    private Long publisherId;
    @PrePersist void assignId() { if (id == null) id = UUID.randomUUID().toString(); }
}
