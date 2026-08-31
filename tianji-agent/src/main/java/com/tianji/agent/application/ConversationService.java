package com.tianji.agent.application;

import com.tianji.agent.api.AgentException;
import com.tianji.agent.api.ConversationDtos.*;
import com.tianji.agent.domain.*;
import com.tianji.agent.memory.ConversationChatMemoryStore;
import com.tianji.agent.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final FeedbackRepository feedbackRepository;
    private final ConversationChatMemoryStore memoryStore;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CitationRepository citationRepository,
            FeedbackRepository feedbackRepository,
            ConversationChatMemoryStore memoryStore
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.feedbackRepository = feedbackRepository;
        this.memoryStore = memoryStore;
    }

    @Transactional
    public ConversationResponse create(Long userId, CreateConversationRequest request) {
        ConversationEntity entity = new ConversationEntity();
        entity.setUserId(userId);
        entity.setTitle(normalizeTitle(request.title()));
        if (request.scene() != null && !request.scene().isBlank()) {
            entity.setScene(request.scene().trim().toUpperCase());
        }
        return toResponse(conversationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long userId) {
        return conversationRepository
                .findTop50ByUserIdAndStatusOrderByUpdateTimeDesc(userId, ConversationStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> messages(Long userId, String conversationId) {
        requireOwnedConversation(userId, conversationId);
        return messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId).stream()
                .filter(message -> message.getRole() != MessageRole.SYSTEM)
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, String conversationId) {
        ConversationEntity conversation = requireOwnedConversation(userId, conversationId);
        List<String> messageIds = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId)
                .stream().map(MessageEntity::getId).toList();
        if (!messageIds.isEmpty()) {
            citationRepository.deleteByMessageIdIn(messageIds);
            feedbackRepository.deleteByMessageIdIn(messageIds);
            messageRepository.deleteByConversationId(conversationId);
        }
        conversation.setStatus(ConversationStatus.DELETED);
        conversationRepository.save(conversation);
        memoryStore.deleteMessages(conversationId);
    }

    @Transactional
    public void feedback(Long userId, String messageId, FeedbackRequest request) {
        messageRepository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> AgentException.notFound("消息不存在"));
        String rating = request.rating().trim().toUpperCase();
        if (!rating.equals("UP") && !rating.equals("DOWN")) {
            throw AgentException.badRequest("反馈类型只能是 UP 或 DOWN");
        }
        FeedbackEntity entity = feedbackRepository.findByUserIdAndMessageId(userId, messageId)
                .orElseGet(FeedbackEntity::new);
        entity.setUserId(userId);
        entity.setMessageId(messageId);
        entity.setRating(rating);
        entity.setReason(request.reason());
        entity.setComment(request.comment());
        feedbackRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public ConversationEntity requireOwnedConversation(Long userId, String conversationId) {
        return conversationRepository.findByIdAndUserIdAndStatus(conversationId, userId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> AgentException.notFound("会话不存在"));
    }

    @Transactional
    public MessageEntity saveMessage(Long userId, String conversationId, MessageRole role, String content) {
        MessageEntity message = new MessageEntity();
        message.setUserId(userId);
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        MessageEntity saved = messageRepository.save(message);
        conversationRepository.touch(conversationId, userId, LocalDateTime.now());
        return saved;
    }

    @Transactional
    public MessageEntity saveExistingMessage(MessageEntity message) {
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public String summary(Long userId, String conversationId) {
        return requireOwnedConversation(userId, conversationId).getSummary();
    }

    /** Stores only the user's recent learning intents; model and retrieved facts are intentionally excluded. */
    @Transactional
    public void refreshSummary(Long userId, String conversationId, int threshold) {
        ConversationEntity conversation = requireOwnedConversation(userId, conversationId);
        List<MessageEntity> all = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
        if (all.size() <= threshold) return;
        List<String> intents = all.stream().filter(message -> message.getRole() == MessageRole.USER)
                .map(MessageEntity::getContent).filter(content -> content != null && !content.isBlank())
                .skip(Math.max(0, all.stream().filter(message -> message.getRole() == MessageRole.USER).count() - 10))
                .map(content -> content.length() <= 240 ? content : content.substring(0, 240))
                .toList();
        conversation.setSummary("近期学习诉求：\n- " + String.join("\n- ", intents));
        conversationRepository.save(conversation);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "新的学习对话";
        }
        return title.trim();
    }

    private ConversationResponse toResponse(ConversationEntity entity) {
        return new ConversationResponse(
                entity.getId(), entity.getTitle(), entity.getScene(), entity.getStatus().name(),
                entity.getCreateTime(), entity.getUpdateTime()
        );
    }

    private MessageResponse toMessageResponse(MessageEntity entity) {
        List<CitationResponse> citations = citationRepository.findByMessageIdOrderByIdAsc(entity.getId()).stream()
                .map(citation -> new CitationResponse(
                        citation.getChunkId(), citation.getSourceType(), citation.getSourceId(),
                        citation.getCourseId(), citation.getChapterId(), citation.getSectionId(),
                        citation.getStartMoment(), citation.getEndMoment(), citation.getTitle(), citation.getScore()
                ))
                .toList();
        return new MessageResponse(
                entity.getId(), entity.getRole().name(), entity.getContent(), entity.getCreateTime(), citations
        );
    }
}
