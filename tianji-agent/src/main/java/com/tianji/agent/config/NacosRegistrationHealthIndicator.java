package com.tianji.agent.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class NacosRegistrationHealthIndicator implements HealthIndicator {
    private final AgentProperties properties;
    private final NacosRegistration registration;

    public NacosRegistrationHealthIndicator(AgentProperties properties, NacosRegistration registration) {
        this.properties = properties;
        this.registration = registration;
    }

    @Override
    public Health health() {
        if (!properties.getDiscovery().isEnabled()) return Health.unknown().withDetail("enabled", false).build();
        if (registration.isRegistered()) return Health.up().withDetail("service", properties.getDiscovery().getServiceName()).build();
        return Health.down().withDetail("service", properties.getDiscovery().getServiceName())
                .withDetail("error", registration.getLastError() == null ? "not registered" : registration.getLastError()).build();
    }
}
