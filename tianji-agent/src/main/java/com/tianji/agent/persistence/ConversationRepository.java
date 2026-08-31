package com.tianji.agent.persistence;

import com.tianji.agent.domain.ConversationEntity;
import com.tianji.agent.domain.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
    List<ConversationEntity> findTop50ByUserIdAndStatusOrderByUpdateTimeDesc(Long userId, ConversationStatus status);
    Optional<ConversationEntity> findByIdAndUserIdAndStatus(String id, Long userId, ConversationStatus status);

    @Modifying
    @Query("update ConversationEntity c set c.updateTime = :updateTime where c.id = :id and c.userId = :userId")
    void touch(@Param("id") String id, @Param("userId") Long userId, @Param("updateTime") LocalDateTime updateTime);
}
