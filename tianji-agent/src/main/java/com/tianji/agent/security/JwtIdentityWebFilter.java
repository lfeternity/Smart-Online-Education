package com.tianji.agent.security;

import com.tianji.agent.api.RequestIds;
import com.tianji.agent.config.AgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/** Converts the verified Tianji JWT user claim into explicit downstream request context. */
@Component
@Order(-90)
@ConditionalOnProperty(prefix = "agent.security", name = "jwt-enabled", havingValue = "true")
public class JwtIdentityWebFilter implements WebFilter {
    private final AgentProperties properties;

    public JwtIdentityWebFilter(AgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.equals("/api/v1/health") || path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext().map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(optional -> {
            if (optional.isEmpty()) return unauthorized(exchange);
            var securityContext = optional.get();
            if (!(securityContext.getAuthentication() instanceof JwtAuthenticationToken authentication)) {
                return unauthorized(exchange);
            }
            Map<String, Object> user = authentication.getToken().getClaimAsMap("user");
            Long userId = number(user == null ? null : user.get("userId"));
            Long roleId = number(user == null ? null : user.get("roleId"));
            if (userId == null) return unauthorized(exchange);
            ServerWebExchange trusted = exchange.mutate().request(builder -> builder.headers(headers -> {
                headers.set(properties.getSecurity().getUserHeader(), userId.toString());
                if (roleId == null) headers.remove(properties.getSecurity().getRoleHeader());
                else headers.set(properties.getSecurity().getRoleHeader(), roleId.toString());
            })).build();
            String requestId = trusted.getRequest().getHeaders().getFirst("requestId");
            return chain.filter(trusted).contextWrite(context -> {
                var updated = context.put(RequestContextWebFilter.USER_CONTEXT_KEY, userId);
                return requestId == null ? updated : updated.put(RequestIds.CONTEXT_KEY, requestId);
            });
        });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(value.toString()); }
        catch (NumberFormatException ignored) { return null; }
    }
}
