package com.tianji.agent.security;

import com.tianji.agent.config.AgentProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(-80)
public class AgentRateLimitFilter implements WebFilter {
    private record Window(long minute, AtomicInteger count) {}
    private record Day(LocalDate day, AtomicInteger count) {}
    private final AgentProperties properties;
    private final DistributedRateLimitService distributed;
    private final ConcurrentHashMap<Long, Window> minuteCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Day> dayCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> concurrent = new ConcurrentHashMap<>();

    public AgentRateLimitFilter(AgentProperties properties, DistributedRateLimitService distributed) {
        this.properties = properties; this.distributed = distributed;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.getLimits().isEnabled() || !exchange.getRequest().getPath().value().endsWith("messages:stream")) {
            return chain.filter(exchange);
        }
        String raw = exchange.getRequest().getHeaders().getFirst(properties.getSecurity().getUserHeader());
        if (raw == null) return chain.filter(exchange);
        long userId;
        try { userId = Long.parseLong(raw); } catch (NumberFormatException exception) { return chain.filter(exchange); }
        if (!distributed.acquire(userId)) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        long minute = System.currentTimeMillis() / 60_000;
        Window window = minuteCounters.compute(userId, (id, old) -> old == null || old.minute != minute
                ? new Window(minute, new AtomicInteger()) : old);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Day day = dayCounters.compute(userId, (id, old) -> old == null || !old.day.equals(today)
                ? new Day(today, new AtomicInteger()) : old);
        AtomicInteger active = concurrent.computeIfAbsent(userId, id -> new AtomicInteger());
        if (window.count.incrementAndGet() > properties.getLimits().getRequestsPerMinute()
                || day.count.incrementAndGet() > properties.getLimits().getMessagesPerDay()
                || active.incrementAndGet() > properties.getLimits().getConcurrentStreams()) {
            active.decrementAndGet();
            distributed.release(userId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange).doFinally(signal -> {
            active.decrementAndGet();
            distributed.release(userId);
        });
    }
}
