package com.tianji.agent.persistence;

import com.tianji.agent.domain.ToolCallEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallRepository extends JpaRepository<ToolCallEntity, Long> {
}
