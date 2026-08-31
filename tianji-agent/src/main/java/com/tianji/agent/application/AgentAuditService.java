package com.tianji.agent.application;

import com.tianji.agent.domain.*;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.security.DistributedRateLimitService;
import com.tianji.agent.persistence.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class AgentAuditService {
    private final ToolCallRepository tools;
    private final CitationRepository citations;
    private final ModelUsageRepository usage;
    private final MessageRepository messages;
    private final AgentProperties properties;
    private final DistributedRateLimitService rateLimits;

    public AgentAuditService(ToolCallRepository tools, CitationRepository citations,
                             ModelUsageRepository usage, MessageRepository messages, AgentProperties properties,
                             DistributedRateLimitService rateLimits) {
        this.tools = tools; this.citations = citations; this.usage = usage; this.messages = messages;
        this.properties = properties;
        this.rateLimits = rateLimits;
    }

    public void tool(String messageId, Long userId, String name, String arguments, boolean failed, Duration duration) {
        ToolCallEntity entity = new ToolCallEntity(); entity.setMessageId(messageId); entity.setUserId(userId);
        entity.setToolName(name); entity.setArgumentsDigest(digest(arguments == null ? "" : arguments));
        entity.setStatus(failed ? "FAILED" : "SUCCEEDED"); entity.setLatencyMs(duration == null ? null : duration.toMillis());
        entity.setErrorCode(failed ? "TOOL_EXECUTION_FAILED" : null); tools.save(entity);
    }

    public void complete(MessageEntity message, Long userId, String requestId, String scene,
                         ChatResponse response, long latencyMs, List<KnowledgeService.SearchHit> hits) {
        var tokenUsage = response.tokenUsage();
        message.setModel(response.modelName()); message.setLatencyMs(latencyMs);
        message.setInputTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount());
        message.setOutputTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount());
        message.setFinishReason(response.finishReason() == null ? "STOP" : response.finishReason().name());
        messages.save(message);
        for (KnowledgeService.SearchHit hit : hits.stream().distinct().toList()) {
            CitationEntity citation = new CitationEntity(); citation.setMessageId(message.getId());
            citation.setChunkId(hit.chunkId()); citation.setSourceType(hit.sourceType()); citation.setSourceId(hit.sourceId());
            citation.setCourseId(hit.courseId()); citation.setChapterId(hit.chapterId()); citation.setSectionId(hit.sectionId());
            citation.setStartMoment(hit.startMoment()); citation.setEndMoment(hit.endMoment());
            citation.setTitle(hit.title()); citation.setScore(hit.score()); citations.save(citation);
        }
        ModelUsageEntity modelUsage = new ModelUsageEntity(); modelUsage.setRequestId(requestId); modelUsage.setUserId(userId);
        modelUsage.setScene(scene); modelUsage.setModel(response.modelName() == null ? "unknown" : response.modelName());
        modelUsage.setInputTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount());
        modelUsage.setOutputTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount());
        modelUsage.setLatencyMs(latencyMs);
        long input = tokenUsage == null || tokenUsage.inputTokenCount() == null ? 0 : tokenUsage.inputTokenCount();
        long output = tokenUsage == null || tokenUsage.outputTokenCount() == null ? 0 : tokenUsage.outputTokenCount();
        // Prices are configured in micros per 1K tokens; zero means unknown and keeps the cost explicitly zero.
        long inputPrice = properties.getAi().getInputPriceMicrosPer1k();
        long outputPrice = properties.getAi().getOutputPriceMicrosPer1k();
        long costMicros = (input * inputPrice + output * outputPrice) / 1000L;
        modelUsage.setEstimatedCostMicros(costMicros);
        modelUsage.setPriceVersion(properties.getAi().getPriceVersion());
        usage.save(modelUsage);
        rateLimits.recordUsage(userId, input + output, costMicros);
    }

    private String digest(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
