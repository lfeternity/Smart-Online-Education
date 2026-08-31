package com.tianji.agent.security;

import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

@Component
public class AgentAuthorization {
    private final AgentProperties properties;

    public AgentAuthorization(AgentProperties properties) {
        this.properties = properties;
    }

    public boolean isAdmin(Long userId, Long roleId) {
        return properties.getSecurity().getAdminUserIds().contains(userId)
                || (roleId != null && properties.getSecurity().getAdminRoleIds().contains(roleId));
    }

    public void requireAdmin(Long userId, Long roleId, String message) {
        if (!isAdmin(userId, roleId)) throw AgentException.forbidden(message);
    }

    public boolean canUploadKnowledge(Long userId, Long roleId) {
        return isAdmin(userId, roleId)
                || (roleId != null && properties.getSecurity().getTeacherRoleIds().contains(roleId));
    }

    public void requireKnowledgeUploader(Long userId, Long roleId) {
        if (!canUploadKnowledge(userId, roleId)) throw AgentException.forbidden("需要教师或知识管理员权限");
    }
}
