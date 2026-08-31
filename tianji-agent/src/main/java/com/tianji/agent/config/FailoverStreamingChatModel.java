package com.tianji.agent.config;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.*;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Fails over only before any content or tool call is emitted, so a request is never replayed after side effects. */
public class FailoverStreamingChatModel implements StreamingChatModel {
    private final StreamingChatModel primary;
    private final StreamingChatModel fallback;
    private final int failureThreshold;
    private final long openMillis;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntil = new AtomicLong();

    public FailoverStreamingChatModel(StreamingChatModel primary, StreamingChatModel fallback,
                                      int failureThreshold, Duration openDuration) {
        this.primary = primary;
        this.fallback = fallback;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openMillis = Math.max(1000, openDuration.toMillis());
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        if (isOpen()) {
            fallback.doChat(request, handler);
            return;
        }
        AtomicBoolean emitted = new AtomicBoolean();
        StreamingChatResponseHandler guarded = new ForwardingHandler(handler, emitted) {
            @Override
            public void onCompleteResponse(ChatResponse response) {
                consecutiveFailures.set(0);
                openUntil.set(0);
                super.onCompleteResponse(response);
            }

            @Override
            public void onError(Throwable error) {
                recordFailure();
                if (emitted.compareAndSet(false, true)) {
                    try {
                        fallback.doChat(request, handler);
                    } catch (Throwable fallbackError) {
                        handler.onError(fallbackError);
                    }
                } else {
                    handler.onError(error);
                }
            }
        };
        try {
            primary.doChat(request, guarded);
        } catch (Throwable error) {
            guarded.onError(error);
        }
    }

    @Override public ChatRequestParameters defaultRequestParameters() { return primary.defaultRequestParameters(); }
    @Override public Set<Capability> supportedCapabilities() { return primary.supportedCapabilities(); }
    @Override public ModelProvider provider() { return primary.provider(); }

    boolean isOpen() { return System.currentTimeMillis() < openUntil.get(); }

    private void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openUntil.set(System.currentTimeMillis() + openMillis);
        }
    }

    private static class ForwardingHandler implements StreamingChatResponseHandler {
        private final StreamingChatResponseHandler delegate;
        private final AtomicBoolean emitted;
        private ForwardingHandler(StreamingChatResponseHandler delegate, AtomicBoolean emitted) {
            this.delegate = delegate;
            this.emitted = emitted;
        }
        @Override public void onPartialResponse(String partialResponse) { emitted.set(true); delegate.onPartialResponse(partialResponse); }
        @Override public void onPartialResponse(PartialResponse response, PartialResponseContext context) { emitted.set(true); delegate.onPartialResponse(response, context); }
        @Override public void onPartialThinking(PartialThinking thinking) { emitted.set(true); delegate.onPartialThinking(thinking); }
        @Override public void onPartialThinking(PartialThinking thinking, PartialThinkingContext context) { emitted.set(true); delegate.onPartialThinking(thinking, context); }
        @Override public void onPartialToolCall(PartialToolCall toolCall) { emitted.set(true); delegate.onPartialToolCall(toolCall); }
        @Override public void onPartialToolCall(PartialToolCall toolCall, PartialToolCallContext context) { emitted.set(true); delegate.onPartialToolCall(toolCall, context); }
        @Override public void onCompleteToolCall(CompleteToolCall toolCall) { emitted.set(true); delegate.onCompleteToolCall(toolCall); }
        @Override public void onUnmappedRawEvent(Object event) { delegate.onUnmappedRawEvent(event); }
        @Override public void onCompleteResponse(ChatResponse response) { delegate.onCompleteResponse(response); }
        @Override public void onError(Throwable error) { delegate.onError(error); }
    }
}
