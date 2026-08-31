package com.tianji.agent.security;

import com.tianji.agent.config.AgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.List;

/** Optional second-layer JWT validation for deployments where Agent is reachable beyond the gateway. */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(prefix = "agent.security", name = "jwt-enabled", havingValue = "true")
public class AgentSecurityConfiguration {

    @Bean
    ReactiveJwtDecoder jwtDecoder(AgentProperties properties) {
        String jwkSetUri = properties.getSecurity().getJwkSetUri();
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new IllegalStateException("agent.security.jwk-set-uri is required when JWT validation is enabled");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        String issuer = properties.getSecurity().getIssuerUri();
        validators.add(issuer == null || issuer.isBlank()
                ? JwtValidators.createDefault() : JwtValidators.createDefaultWithIssuer(issuer));
        String audience = properties.getSecurity().getRequiredAudience();
        if (audience != null && !audience.isBlank()) {
            validators.add(jwt -> jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing", null)));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain agentSecurityChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }))
                .build();
    }
}
