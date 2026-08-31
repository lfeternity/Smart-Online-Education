package com.tianji.agent.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record CreateConversationRequest(@Size(max = 120) String title, String scene) {
    }

    public record ConversationResponse(
            String id,
            String title,
            String scene,
            String status,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
    }

    public record MessageResponse(
            String id,
            String role,
            String content,
            LocalDateTime createTime,
            List<CitationResponse> citations
    ) {
    }

    public record CitationResponse(
            String chunkId,
            String sourceType,
            String sourceId,
            Long courseId,
            Long chapterId,
            Long sectionId,
            Integer startMoment,
            Integer endMoment,
            String title,
            Double score
    ) {
    }

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String message,
            @Valid ChatContext context
    ) {
    }

    public record ChatContext(
            Long courseId,
            Long chapterId,
            Long sectionId,
            @Min(0) @Max(86400) Integer playMoment,
            @Size(max = 32) String page
    ) {
    }

    public record FeedbackRequest(
            @NotBlank String rating,
            @Size(max = 64) String reason,
            @Size(max = 500) String comment
    ) {
    }

    public record PendingActionResponse(
            String id,
            String actionType,
            String summary,
            String status,
            LocalDateTime expireTime
    ) {
    }
}
