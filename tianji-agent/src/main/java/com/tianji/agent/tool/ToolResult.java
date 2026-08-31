package com.tianji.agent.tool;

public record ToolResult<T>(boolean success, String code, T data, String message, String traceId) {
    public static <T> ToolResult<T> ok(T data, String traceId) {
        return new ToolResult<>(true, "OK", data, null, traceId);
    }
}
