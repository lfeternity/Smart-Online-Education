package com.tianji.agent.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** Keeps the gateway-header authentication path active for local/MVP deployments. */
@Configuration
@ConditionalOnProperty(prefix = "agent.security", name = "jwt-enabled", havingValue = "false", matchIfMissing = true)
public class AgentSecurityFallbackConfiguration {
    @Bean
    SecurityWebFilterChain agentHeaderSecurityChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
}
