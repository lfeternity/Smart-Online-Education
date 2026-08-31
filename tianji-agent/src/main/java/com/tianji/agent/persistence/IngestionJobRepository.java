package com.tianji.agent.persistence;

import com.tianji.agent.domain.IngestionJobEntity;
import com.tianji.agent.domain.IngestionJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, String> {
    List<IngestionJobEntity> findTop20ByStatusInAndNextAttemptTimeLessThanEqualOrderByCreateTimeAsc(
            Collection<IngestionJobStatus> statuses, LocalDateTime due);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from IngestionJobEntity job where job.id = :id")
    java.util.Optional<IngestionJobEntity> findByIdForUpdate(String id);
}
