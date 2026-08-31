package com.tianji.agent.persistence;

import com.tianji.agent.domain.KnowledgeDocumentEntity;
import com.tianji.agent.domain.KnowledgeStatus;
import com.tianji.agent.domain.KnowledgeSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, String> {
    List<KnowledgeDocumentEntity> findTop100ByOrderByUpdateTimeDesc();
    List<KnowledgeDocumentEntity> findByCourseIdAndStatus(Long courseId, KnowledgeStatus status);
    List<KnowledgeDocumentEntity> findByCourseIdAndSourceTypeAndSourceIdAndStatus(
            Long courseId, KnowledgeSourceType sourceType, String sourceId, KnowledgeStatus status);
    Optional<KnowledgeDocumentEntity> findTopByCourseIdAndSourceTypeAndSourceIdOrderByVersionDesc(
            Long courseId, KnowledgeSourceType sourceType, String sourceId);
    Optional<KnowledgeDocumentEntity> findFirstByCourseIdAndSourceTypeAndSourceIdAndContentHash(
            Long courseId, com.tianji.agent.domain.KnowledgeSourceType sourceType, String sourceId, String contentHash);
}
