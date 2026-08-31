package com.tianji.agent.api;

public record ApiResponse<T>(int code, String msg, T data, String requestId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data, RequestIds.current());
    }

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(200, "OK", data,
                requestId == null || requestId.isBlank() ? RequestIds.current() : requestId);
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, RequestIds.current());
    }
}
