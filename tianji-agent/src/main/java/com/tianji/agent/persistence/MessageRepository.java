package com.tianji.agent.persistence;

import com.tianji.agent.domain.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {
    List<MessageEntity> findByConversationIdOrderByCreateTimeAsc(String conversationId);
    Optional<MessageEntity> findByIdAndUserId(String id, Long userId);
    void deleteByConversationId(String conversationId);
    long countByConversationId(String conversationId);
}
