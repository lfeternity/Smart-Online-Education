package com.tianji.agent.persistence;

import com.tianji.agent.domain.PendingActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PendingActionRepository extends JpaRepository<PendingActionEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingActionEntity> findByIdAndUserId(String id, Long userId);
    Optional<PendingActionEntity> findByIdempotencyKey(String idempotencyKey);
}
