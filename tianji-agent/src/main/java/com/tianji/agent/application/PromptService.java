package com.tianji.agent.application;

import com.tianji.agent.ai.AgentPrompt;
import com.tianji.agent.api.AgentException;
import com.tianji.agent.domain.PromptVersionEntity;
import com.tianji.agent.persistence.PromptVersionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class PromptService {
    public static final String LEARNING_AGENT = "learning-agent";
    public record PromptSnapshot(String version, String content) {}
    private final PromptVersionRepository repository;

    public PromptService(PromptVersionRepository repository) { this.repository = repository; }

    @PostConstruct
    void seed() {
        if (repository.findFirstByPromptKeyAndStatusOrderByUpdateTimeDesc(LEARNING_AGENT, "ACTIVE").isEmpty()) {
            PromptVersionEntity entity = new PromptVersionEntity(); entity.setPromptKey(LEARNING_AGENT);
            entity.setVersion(AgentPrompt.VERSION); entity.setContent(AgentPrompt.SYSTEM);
            entity.setContentHash(digest(AgentPrompt.SYSTEM)); entity.setStatus("ACTIVE"); repository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public PromptSnapshot active() {
        return repository.findFirstByPromptKeyAndStatusOrderByUpdateTimeDesc(LEARNING_AGENT, "ACTIVE")
                .map(value -> new PromptSnapshot(value.getVersion(), value.getContent()))
                .orElse(new PromptSnapshot(AgentPrompt.VERSION, AgentPrompt.SYSTEM));
    }

    @Transactional(readOnly = true)
    public List<PromptVersionEntity> list() { return repository.findByPromptKeyOrderByCreateTimeDesc(LEARNING_AGENT); }

    @Transactional
    public PromptVersionEntity create(String version, String content) {
        if (version == null || version.isBlank() || content == null || content.isBlank()) throw AgentException.badRequest("版本和提示词不能为空");
        PromptVersionEntity entity = new PromptVersionEntity(); entity.setPromptKey(LEARNING_AGENT);
        entity.setVersion(version.trim()); entity.setContent(content); entity.setContentHash(digest(content));
        return repository.save(entity);
    }

    @Transactional
    public PromptVersionEntity publish(String id, Long publisherId) {
        PromptVersionEntity selected = repository.findById(id).orElseThrow(() -> AgentException.notFound("Prompt 版本不存在"));
        for (PromptVersionEntity value : repository.findByPromptKeyOrderByCreateTimeDesc(selected.getPromptKey())) {
            if ("ACTIVE".equals(value.getStatus())) { value.setStatus("ARCHIVED"); repository.save(value); }
        }
        selected.setStatus("ACTIVE"); selected.setPublisherId(publisherId); return repository.save(selected);
    }

    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
