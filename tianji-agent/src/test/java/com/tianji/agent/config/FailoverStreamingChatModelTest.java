package com.tianji.agent.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailoverStreamingChatModelTest {
    private final ChatRequest request = ChatRequest.builder().messages(UserMessage.from("hello")).build();

    @Test
    void failsOverBeforeOutput() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        StreamingChatModel model = new FailoverStreamingChatModel(errorModel(false, new AtomicInteger()),
                successModel(fallbackCalls), 3, Duration.ofSeconds(30));
        AtomicInteger completions = new AtomicInteger();

        model.doChat(request, handler(completions));

        assertEquals(1, fallbackCalls.get());
        assertEquals(1, completions.get());
    }

    @Test
    void neverReplaysAfterPartialOutput() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        StreamingChatModel model = new FailoverStreamingChatModel(errorModel(true, new AtomicInteger()),
                successModel(fallbackCalls), 3, Duration.ofSeconds(30));

        model.doChat(request, new StreamingChatResponseHandler() {
            @Override public void onCompleteResponse(ChatResponse response) { }
            @Override public void onError(Throwable error) { errors.incrementAndGet(); }
        });

        assertEquals(0, fallbackCalls.get());
        assertEquals(1, errors.get());
    }

    @Test
    void opensCircuitAfterThreshold() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        StreamingChatModel model = new FailoverStreamingChatModel(errorModel(false, primaryCalls),
                successModel(fallbackCalls), 2, Duration.ofSeconds(30));

        model.doChat(request, handler(new AtomicInteger()));
        model.doChat(request, handler(new AtomicInteger()));
        model.doChat(request, handler(new AtomicInteger()));

        assertEquals(2, primaryCalls.get());
        assertEquals(3, fallbackCalls.get());
    }

    private StreamingChatModel errorModel(boolean emitFirst, AtomicInteger calls) {
        return new StreamingChatModel() {
            @Override public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                calls.incrementAndGet();
                if (emitFirst) handler.onPartialResponse("partial");
                handler.onError(new IllegalStateException("failed"));
            }
        };
    }

    private StreamingChatModel successModel(AtomicInteger calls) {
        return new StreamingChatModel() {
            @Override public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                calls.incrementAndGet();
                handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("ok")).build());
            }
        };
    }

    private StreamingChatResponseHandler handler(AtomicInteger completions) {
        return new StreamingChatResponseHandler() {
            @Override public void onCompleteResponse(ChatResponse response) { completions.incrementAndGet(); }
            @Override public void onError(Throwable error) { }
        };
    }
}
