package com.tianji.agent.security;

import com.tianji.agent.api.RequestIds;
import com.tianji.agent.config.AgentProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextWebFilter implements WebFilter {

    public static final String USER_CONTEXT_KEY = "agentUserId";
    private final AgentProperties properties;

    public RequestContextWebFilter(AgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = RequestIds.create();
        }
        exchange.getResponse().getHeaders().set("requestId", requestId);
        String requestHeaderId = requestId;
        exchange = exchange.mutate().request(builder -> builder.headers(headers -> headers.set("requestId", requestHeaderId))).build();

        String path = exchange.getRequest().getPath().value();
        boolean publicPath = path.startsWith("/actuator/health") || path.equals("/api/v1/health");
        String rawUserId = exchange.getRequest().getHeaders().getFirst(properties.getSecurity().getUserHeader());

        if (properties.getSecurity().isJwtEnabled()) {
            return chain.filter(exchange).contextWrite(context -> context.put(RequestIds.CONTEXT_KEY, requestHeaderId));
        }

        if (!publicPath && (rawUserId == null || rawUserId.isBlank())) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = null;
        if (rawUserId != null && !rawUserId.isBlank()) {
            try {
                userId = Long.valueOf(rawUserId);
            } catch (NumberFormatException exception) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        String finalRequestId = requestId;
        Long finalUserId = userId;
        return chain.filter(exchange)
                .contextWrite(context -> {
                    var updated = context.put(RequestIds.CONTEXT_KEY, finalRequestId);
                    return finalUserId == null ? updated : updated.put(USER_CONTEXT_KEY, finalUserId);
                });
    }
}
