package com.tianji.agent.persistence;

import com.tianji.agent.domain.ModelUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelUsageRepository extends JpaRepository<ModelUsageEntity, Long> {
}
