package com.tianji.agent.api;

import reactor.util.context.ContextView;

import java.util.UUID;

public final class RequestIds {

    public static final String CONTEXT_KEY = "requestId";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestIds() {
    }

    public static String current() {
        String value = CURRENT.get();
        return value == null ? "" : value;
    }

    public static String from(ContextView contextView) {
        return contextView.getOrDefault(CONTEXT_KEY, "");
    }

    public static String create() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void set(String requestId) {
        CURRENT.set(requestId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
