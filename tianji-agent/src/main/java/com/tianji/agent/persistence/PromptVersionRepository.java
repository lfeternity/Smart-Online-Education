package com.tianji.agent.persistence;

import com.tianji.agent.domain.PromptVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PromptVersionRepository extends JpaRepository<PromptVersionEntity, String> {
    Optional<PromptVersionEntity> findFirstByPromptKeyAndStatusOrderByUpdateTimeDesc(String promptKey, String status);
    List<PromptVersionEntity> findByPromptKeyOrderByCreateTimeDesc(String promptKey);
}
