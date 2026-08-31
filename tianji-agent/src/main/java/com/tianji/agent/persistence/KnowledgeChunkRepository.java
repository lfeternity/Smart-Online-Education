package com.tianji.agent.persistence;

import com.tianji.agent.domain.KnowledgeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, String> {
    List<KnowledgeChunkEntity> findByDocumentIdOrderByChunkIndexAsc(String documentId);
    List<KnowledgeChunkEntity> findTop500ByCourseIdAndActiveTrue(Long courseId);
    List<KnowledgeChunkEntity> findTop500ByActiveTrue();
    void deleteByDocumentId(String documentId);
}
