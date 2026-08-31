package com.tianji.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.api.ConversationDtos.PendingActionResponse;
import com.tianji.agent.client.BusinessClients;
import com.tianji.agent.domain.PendingActionEntity;
import com.tianji.agent.domain.PendingActionStatus;
import com.tianji.agent.domain.PendingActionType;
import com.tianji.agent.persistence.PendingActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PendingActionService {

    private final PendingActionRepository repository;
    private final BusinessClients clients;
    private final ObjectMapper objectMapper;

    public PendingActionService(PendingActionRepository repository, BusinessClients clients, ObjectMapper objectMapper) {
        this.repository = repository;
        this.clients = clients;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PendingActionResponse prepare(Long userId, String conversationId, PendingActionType type,
                                         JsonNode payload, String summary) {
        validate(type, payload);
        String serialized = payload.toString();
        String key = digest(userId + ":" + conversationId + ":" + type + ":" + serialized);
        PendingActionEntity entity = repository.findByIdempotencyKey(key).orElseGet(PendingActionEntity::new);
        if (entity.getStatus() == PendingActionStatus.CONFIRMED) {
            return toResponse(entity);
        }
        entity.setUserId(userId);
        entity.setConversationId(conversationId);
        entity.setActionType(type);
        entity.setPayload(serialized);
        entity.setStatus(PendingActionStatus.PENDING);
        entity.setExpireTime(LocalDateTime.now().plusMinutes(10));
        entity.setIdempotencyKey(key);
        entity.setResultMessage(summary);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public PendingActionResponse confirm(Long userId, String requestId, String actionId) {
        PendingActionEntity action = requireOwned(userId, actionId);
        if (action.getStatus() == PendingActionStatus.CONFIRMED) {
            return toResponse(action);
        }
        if (action.getStatus() != PendingActionStatus.PENDING) {
            throw AgentException.conflict("该操作已处理");
        }
        if (!action.getExpireTime().isAfter(LocalDateTime.now())) {
            action.setStatus(PendingActionStatus.EXPIRED);
            repository.save(action);
            throw AgentException.conflict("该确认操作已过期");
        }
        JsonNode payload = read(action.getPayload());
        validate(action.getActionType(), payload);
        switch (action.getActionType()) {
            case CREATE_LEARNING_PLAN -> clients.createLearningPlan(userId, requestId, payload);
            case CREATE_NOTE -> clients.createNote(userId, requestId, payload);
            case CREATE_QUESTION -> clients.createQuestion(userId, requestId, payload);
        }
        action.setStatus(PendingActionStatus.CONFIRMED);
        action.setResultMessage("操作已执行");
        return toResponse(repository.save(action));
    }

    @Transactional
    public PendingActionResponse cancel(Long userId, String actionId) {
        PendingActionEntity action = requireOwned(userId, actionId);
        if (action.getStatus() == PendingActionStatus.PENDING) {
            action.setStatus(PendingActionStatus.CANCELLED);
            action.setResultMessage("操作已取消");
            repository.save(action);
        }
        return toResponse(action);
    }

    private PendingActionEntity requireOwned(Long userId, String id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> AgentException.notFound("待确认操作不存在"));
    }

    private void validate(PendingActionType type, JsonNode payload) {
        switch (type) {
            case CREATE_LEARNING_PLAN -> {
                positive(payload, "courseId");
                int freq = payload.path("freq").asInt();
                if (freq < 1 || freq > 50) throw AgentException.badRequest("每周学习频率应为 1 到 50");
            }
            case CREATE_NOTE -> {
                positive(payload, "courseId"); positive(payload, "chapterId"); positive(payload, "sectionId");
                if (payload.path("content").asText("").isBlank()) throw AgentException.badRequest("笔记内容不能为空");
                if (payload.path("noteMoment").asInt(-1) < 0) throw AgentException.badRequest("笔记时间无效");
            }
            case CREATE_QUESTION -> {
                positive(payload, "courseId"); positive(payload, "chapterId"); positive(payload, "sectionId");
                if (payload.path("title").asText("").isBlank() || payload.path("description").asText("").isBlank()) {
                    throw AgentException.badRequest("问题标题和描述不能为空");
                }
            }
        }
    }

    private void positive(JsonNode payload, String field) {
        if (payload.path(field).asLong(0) <= 0) throw AgentException.badRequest(field + " 无效");
    }

    private JsonNode read(String value) {
        try { return objectMapper.readTree(value); }
        catch (Exception exception) { throw AgentException.badRequest("待确认参数无效"); }
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private PendingActionResponse toResponse(PendingActionEntity entity) {
        return new PendingActionResponse(entity.getId(), entity.getActionType().name(), entity.getResultMessage(),
                entity.getStatus().name(), entity.getExpireTime());
    }
}
