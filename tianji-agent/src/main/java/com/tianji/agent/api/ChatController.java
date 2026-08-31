package com.tianji.agent.api;

import com.tianji.agent.api.ConversationDtos.ChatRequest;
import com.tianji.agent.application.AgentChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/conversations")
public class ChatController {
    private final AgentChatService service;

    public ChatController(AgentChatService service) { this.service = service; }

    @PostMapping(value = "/{id}/messages:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatEvent>> stream(@RequestHeader("user-info") Long userId,
                                                   @RequestHeader(value = "requestId", required = false) String requestId,
                                                   @PathVariable("id") String conversationId,
                                                   @Valid @RequestBody ChatRequest request) {
        return service.stream(userId, conversationId, requestId, request);
    }
}
