package com.tianji.agent.api;

import lombok.Getter;

@Getter
public class AgentException extends RuntimeException {

    private final int code;

    public AgentException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static AgentException badRequest(String message) {
        return new AgentException(400, message);
    }

    public static AgentException unauthorized() {
        return new AgentException(401, "请先登录");
    }

    public static AgentException forbidden(String message) {
        return new AgentException(403, message);
    }

    public static AgentException notFound(String message) {
        return new AgentException(404, message);
    }

    public static AgentException conflict(String message) {
        return new AgentException(409, message);
    }

    public static AgentException tooManyRequests(String message) {
        return new AgentException(429, message);
    }

    public static AgentException unavailable(String message) {
        return new AgentException(503, message);
    }
}
