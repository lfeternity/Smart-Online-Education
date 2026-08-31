package com.tianji.agent.api;

import com.tianji.agent.config.AgentProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final AgentProperties properties;

    public HealthController(AgentProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health(@RequestHeader(value = "requestId", required = false) String requestId) {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "aiEnabled", properties.getAi().isEnabled(),
                "qdrantEnabled", properties.getQdrant().isEnabled()
        ), requestId);
    }
}
