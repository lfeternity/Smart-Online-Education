package com.tianji.agent.security;

import com.tianji.agent.config.AgentProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/** Redis-backed counters used when multiple Agent instances share traffic. */
@Service
public class DistributedRateLimitService {
    private final ReactiveStringRedisTemplate redis;
    private final AgentProperties properties;

    public DistributedRateLimitService(ReactiveStringRedisTemplate redis, AgentProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.getLimits().isEnabled() && properties.getLimits().isRedisEnabled();
    }

    public boolean acquire(Long userId) {
        if (!enabled()) return true;
        String day = day();
        long minute = System.currentTimeMillis() / 60_000;
        try {
            Long minuteCount = redis.opsForValue().increment("agent:limit:minute:" + userId + ":" + minute).block();
            redis.expire("agent:limit:minute:" + userId + ":" + minute, Duration.ofMinutes(2)).block();
            Long dayCount = increment("agent:limit:messages:" + userId + ":" + day, 1);
            Long concurrent = increment("agent:limit:concurrent:" + userId, 1);
            long userTokens = value("agent:usage:tokens:" + userId + ":" + day);
            long globalTokens = value("agent:usage:tokens:global:" + day);
            long userCost = value("agent:usage:cost:" + userId + ":" + day);
            long globalCost = value("agent:usage:cost:global:" + day);
            boolean allowed = minuteCount != null && dayCount != null && concurrent != null
                    && minuteCount <= properties.getLimits().getRequestsPerMinute()
                    && dayCount <= properties.getLimits().getMessagesPerDay()
                    && concurrent <= properties.getLimits().getConcurrentStreams()
                    && userTokens < properties.getLimits().getTokensPerDay()
                    && globalTokens < properties.getLimits().getGlobalTokensPerDay()
                    && belowOptionalBudget(userCost, properties.getLimits().getCostMicrosPerDay())
                    && belowOptionalBudget(globalCost, properties.getLimits().getGlobalCostMicrosPerDay());
            if (!allowed) release(userId);
            return allowed;
        } catch (RuntimeException ignored) {
            // Availability of the assistant must not depend on Redis being healthy.
            return true;
        }
    }

    public void release(Long userId) {
        if (!enabled()) return;
        try {
            String key = "agent:limit:concurrent:" + userId;
            Long value = redis.opsForValue().decrement(key).block();
            if (value != null && value < 0) redis.opsForValue().set(key, "0", Duration.ofMinutes(10)).block();
        } catch (RuntimeException ignored) { }
    }

    public void recordUsage(Long userId, long tokens, long costMicros) {
        if (!enabled()) return;
        String day = day();
        try {
            increment("agent:usage:tokens:" + userId + ":" + day, Math.max(0, tokens));
            increment("agent:usage:tokens:global:" + day, Math.max(0, tokens));
            increment("agent:usage:cost:" + userId + ":" + day, Math.max(0, costMicros));
            increment("agent:usage:cost:global:" + day, Math.max(0, costMicros));
        } catch (RuntimeException ignored) { }
    }

    private Long increment(String key, long amount) {
        Long result = redis.opsForValue().increment(key, amount).block();
        redis.expire(key, Duration.ofDays(2)).block();
        return result;
    }

    private long value(String key) {
        String raw = redis.opsForValue().get(key).block();
        try { return raw == null ? 0 : Long.parseLong(raw); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private boolean belowOptionalBudget(long value, long limit) { return limit <= 0 || value < limit; }
    private String day() { return LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(); }
}
