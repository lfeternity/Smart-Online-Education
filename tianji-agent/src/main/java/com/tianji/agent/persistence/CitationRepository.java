package com.tianji.agent.persistence;

import com.tianji.agent.domain.CitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitationRepository extends JpaRepository<CitationEntity, Long> {
    List<CitationEntity> findByMessageIdOrderByIdAsc(String messageId);
    void deleteByMessageIdIn(java.util.Collection<String> messageIds);
}
