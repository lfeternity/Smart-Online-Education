package com.tianji.agent.security;

import com.tianji.agent.api.AgentException;
import com.tianji.agent.config.AgentProperties;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentAuthorizationTest {
    @Test
    void teacherCanUploadButCannotPublishAsAdmin() {
        AgentProperties properties = new AgentProperties();
        properties.getSecurity().setTeacherRoleIds(Set.of(20L));
        properties.getSecurity().setAdminRoleIds(Set.of(30L));
        AgentAuthorization authorization = new AgentAuthorization(properties);

        assertTrue(authorization.canUploadKnowledge(7L, 20L));
        assertThrows(AgentException.class,
                () -> authorization.requireAdmin(7L, 20L, "admin required"));
        assertDoesNotThrow(() -> authorization.requireAdmin(8L, 30L, "admin required"));
    }
}
