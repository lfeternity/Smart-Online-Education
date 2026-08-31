package com.tianji.agent.persistence;

import com.tianji.agent.domain.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> { }
