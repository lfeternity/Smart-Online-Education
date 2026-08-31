package com.tianji.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_user_profile")
public class UserProfileEntity extends AuditedEntity {
    @Id
    private Long userId;

    @Column(length = 500)
    private String learningGoal;

    @Column(length = 32)
    private String preferredStyle;

    private Integer weeklyHours;

    @Column(nullable = false)
    private Boolean consented = false;
}
