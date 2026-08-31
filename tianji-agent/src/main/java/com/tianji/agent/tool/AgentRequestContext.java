package com.tianji.agent.tool;

import com.tianji.agent.api.ConversationDtos.ChatContext;

public record AgentRequestContext(Long userId, String requestId, String conversationId, String messageId, ChatContext page) {
    public Long courseIdOr(Long requestedCourseId) {
        return requestedCourseId != null ? requestedCourseId : page == null ? null : page.courseId();
    }
}
