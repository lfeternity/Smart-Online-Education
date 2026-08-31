package com.tianji.agent.persistence;

import com.tianji.agent.domain.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {
    Optional<FeedbackEntity> findByUserIdAndMessageId(Long userId, String messageId);
    void deleteByMessageIdIn(java.util.Collection<String> messageIds);
}
