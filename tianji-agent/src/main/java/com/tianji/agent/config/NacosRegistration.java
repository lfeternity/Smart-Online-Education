package com.tianji.agent.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class NacosRegistration implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {
    private final AgentProperties properties;
    private final Environment environment;
    private final AtomicBoolean registered = new AtomicBoolean();
    private volatile NamingService namingService;
    private volatile Instance instance;
    private volatile String lastError;

    public NacosRegistration(AgentProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.getDiscovery().isEnabled()) return;
        try {
            register();
        } catch (Exception error) {
            lastError = error.getClass().getSimpleName() + ": " + safe(error.getMessage());
            log.error("Nacos registration failed for service {}: {}",
                    properties.getDiscovery().getServiceName(), lastError);
            if (properties.getDiscovery().isFailFast()) {
                throw new IllegalStateException("Nacos registration failed", error);
            }
        }
    }

    void register() throws Exception {
        AgentProperties.Discovery discovery = properties.getDiscovery();
        Properties nacos = new Properties();
        nacos.setProperty("serverAddr", discovery.getServerAddr());
        if (!discovery.getNamespace().isBlank()) nacos.setProperty("namespace", discovery.getNamespace());
        if (!discovery.getUsername().isBlank()) nacos.setProperty("username", discovery.getUsername());
        if (!discovery.getPassword().isBlank()) nacos.setProperty("password", discovery.getPassword());
        namingService = NacosFactory.createNamingService(nacos);
        Instance value = new Instance();
        value.setIp(discovery.getIp().isBlank() ? InetAddress.getLocalHost().getHostAddress() : discovery.getIp());
        value.setPort(discovery.getPort() > 0 ? discovery.getPort()
                : Integer.parseInt(environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8094"))));
        value.setClusterName(discovery.getCluster());
        value.setHealthy(true);
        value.setEnabled(true);
        value.setEphemeral(true);
        HashMap<String, String> metadata = new HashMap<>(discovery.getMetadata());
        metadata.putIfAbsent("protocol", "http");
        metadata.putIfAbsent("management", "/actuator/health");
        value.setMetadata(metadata);
        namingService.registerInstance(discovery.getServiceName(), discovery.getGroup(), value);
        instance = value;
        registered.set(true);
        lastError = null;
        log.info("Registered {} in Nacos at {}:{}", discovery.getServiceName(), value.getIp(), value.getPort());
    }

    void close() {
        NamingService service = namingService;
        Instance value = instance;
        if (service == null) return;
        try {
            if (registered.get() && value != null) {
                AgentProperties.Discovery discovery = properties.getDiscovery();
                service.deregisterInstance(discovery.getServiceName(), discovery.getGroup(), value);
            }
            service.shutDown();
        } catch (Exception error) {
            log.warn("Nacos deregistration failed for {}", properties.getDiscovery().getServiceName());
        } finally {
            registered.set(false);
        }
    }

    @Override
    public void destroy() { close(); }

    public boolean isRegistered() { return registered.get(); }
    public String getLastError() { return lastError; }

    private String safe(String value) {
        if (value == null) return "unknown";
        String clean = value.replaceAll("(?i)(password|token|secret|sk-[a-z0-9_-]{8,})[^\\s,;]*", "[redacted]");
        return clean.length() <= 300 ? clean : clean.substring(0, 300);
    }
}
