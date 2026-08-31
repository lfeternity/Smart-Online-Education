package com.tianji.agent.memory;

import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.MessageEntity;
import com.tianji.agent.domain.MessageRole;
import com.tianji.agent.persistence.MessageRepository;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConversationChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "agent:memory:";

    private final AgentProperties properties;
    private final MessageRepository messageRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, List<ChatMessage>> localCache = new ConcurrentHashMap<>();

    public ConversationChatMemoryStore(
            AgentProperties properties,
            MessageRepository messageRepository,
            ReactiveStringRedisTemplate redisTemplate
    ) {
        this.properties = properties;
        this.messageRepository = messageRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        List<ChatMessage> cached = readCache(id);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        List<MessageEntity> persisted = messageRepository.findByConversationIdOrderByCreateTimeAsc(id);
        // A blank assistant placeholder means the current request is in flight; do not feed that user message twice.
        if (persisted.size() >= 2 && persisted.get(persisted.size() - 1).getRole() == MessageRole.ASSISTANT
                && persisted.get(persisted.size() - 1).getContent().isBlank()
                && persisted.get(persisted.size() - 2).getRole() == MessageRole.USER) {
            persisted = persisted.subList(0, persisted.size() - 2);
        }
        List<ChatMessage> loaded = persisted.stream()
                .filter(message -> message.getRole() != MessageRole.SYSTEM)
                .filter(message -> message.getContent() != null && !message.getContent().isBlank())
                .map(this::toChatMessage)
                .toList();
        loaded = deduplicateAdjacent(loaded);
        if (loaded.size() > properties.getMemory().getMaxMessages()) {
            loaded = loaded.subList(loaded.size() - properties.getMemory().getMaxMessages(), loaded.size());
        }
        updateMessages(id, loaded);
        return new ArrayList<>(loaded);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();
        List<ChatMessage> snapshot = deduplicateAdjacent(messages);
        localCache.put(id, snapshot);
        if (!properties.getMemory().isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.opsForValue()
                    .set(KEY_PREFIX + id, ChatMessageSerializer.messagesToJson(snapshot), properties.getMemory().getTtl())
                    .block();
        } catch (RuntimeException exception) {
            log.warn("Redis chat memory update failed, conversationId={}", id);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        localCache.remove(id);
        if (!properties.getMemory().isRedisEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + id).block();
        } catch (RuntimeException exception) {
            log.warn("Redis chat memory delete failed, conversationId={}", id);
        }
    }

    private List<ChatMessage> readCache(String id) {
        List<ChatMessage> local = localCache.get(id);
        if (local != null) {
            return local;
        }
        if (!properties.getMemory().isRedisEnabled()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + id).block();
            if (json == null || json.isBlank()) {
                return null;
            }
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
            localCache.put(id, List.copyOf(messages));
            return messages;
        } catch (RuntimeException exception) {
            log.warn("Redis chat memory read failed, conversationId={}", id);
            return null;
        }
    }

    private ChatMessage toChatMessage(MessageEntity entity) {
        return switch (entity.getRole()) {
            case USER -> UserMessage.from(entity.getContent());
            case ASSISTANT -> AiMessage.from(entity.getContent());
            case SYSTEM -> SystemMessage.from(entity.getContent());
        };
    }

    private List<ChatMessage> deduplicateAdjacent(List<ChatMessage> messages) {
        List<ChatMessage> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (!result.isEmpty() && result.get(result.size() - 1).equals(message)) continue;
            result.add(message);
        }
        return List.copyOf(result);
    }
}
