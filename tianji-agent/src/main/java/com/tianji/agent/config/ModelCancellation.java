package com.tianji.agent.config;

import dev.langchain4j.http.client.sse.ServerSentEventParsingHandle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ModelCancellation {
    private static final ThreadLocal<Token> CURRENT = new ThreadLocal<>();

    private ModelCancellation() { }

    public static void runWith(Token token, Runnable action) {
        CURRENT.set(token);
        try { action.run(); }
        finally { CURRENT.remove(); }
    }

    static Token current() { return CURRENT.get(); }

    public static final class Token {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<ServerSentEventParsingHandle> handle = new AtomicReference<>();

        void attach(ServerSentEventParsingHandle value) {
            if (value == null) return;
            handle.set(value);
            if (cancelled.get()) value.cancel();
        }

        public void cancel() {
            cancelled.set(true);
            ServerSentEventParsingHandle value = handle.get();
            if (value != null) value.cancel();
        }

        public boolean isCancelled() { return cancelled.get(); }
    }
}
