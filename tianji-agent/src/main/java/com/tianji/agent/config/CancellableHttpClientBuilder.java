package com.tianji.agent.config;

import dev.langchain4j.http.client.*;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventContext;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.time.Duration;

public class CancellableHttpClientBuilder implements HttpClientBuilder {
    private final JdkHttpClientBuilder delegate = new JdkHttpClientBuilder();

    @Override public Duration connectTimeout() { return delegate.connectTimeout(); }
    @Override public HttpClientBuilder connectTimeout(Duration timeout) { delegate.connectTimeout(timeout); return this; }
    @Override public Duration readTimeout() { return delegate.readTimeout(); }
    @Override public HttpClientBuilder readTimeout(Duration timeout) { delegate.readTimeout(timeout); return this; }

    @Override
    public HttpClient build() {
        HttpClient client = delegate.build();
        return new HttpClient() {
            @Override public SuccessfulHttpResponse execute(HttpRequest request) { return client.execute(request); }

            @Override
            public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
                ModelCancellation.Token token = ModelCancellation.current();
                client.execute(request, parser, token == null ? listener : new ServerSentEventListener() {
                    @Override public void onOpen(SuccessfulHttpResponse response) { listener.onOpen(response); }
                    @Override public void onEvent(ServerSentEvent event, ServerSentEventContext context) {
                        token.attach(context.parsingHandle());
                        if (!token.isCancelled()) listener.onEvent(event, context);
                    }
                    @Override public void onEvent(ServerSentEvent event) {
                        if (!token.isCancelled()) listener.onEvent(event);
                    }
                    @Override public void onError(Throwable error) {
                        if (!token.isCancelled()) listener.onError(error);
                    }
                    @Override public void onClose() { listener.onClose(); }
                });
            }
        };
    }
}
