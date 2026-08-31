package com.tianji.agent.api;

import com.tianji.agent.api.ConversationDtos.*;
import com.tianji.agent.application.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public Mono<ApiResponse<ConversationResponse>> create(
            @RequestHeader("user-info") Long userId,
            @RequestHeader(value = "requestId", required = false) String requestId,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return blocking(() -> ApiResponse.ok(conversationService.create(userId, request), requestId));
    }

    @GetMapping
    public Mono<ApiResponse<List<ConversationResponse>>> list(@RequestHeader("user-info") Long userId,
                                                               @RequestHeader(value = "requestId", required = false) String requestId) {
        return blocking(() -> ApiResponse.ok(conversationService.list(userId), requestId));
    }

    @GetMapping("/{id}/messages")
    public Mono<ApiResponse<List<MessageResponse>>> messages(
            @RequestHeader("user-info") Long userId,
            @RequestHeader(value = "requestId", required = false) String requestId,
            @PathVariable("id") String conversationId
    ) {
        return blocking(() -> ApiResponse.ok(conversationService.messages(userId, conversationId), requestId));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(
            @RequestHeader("user-info") Long userId,
            @RequestHeader(value = "requestId", required = false) String requestId,
            @PathVariable("id") String conversationId
    ) {
        return blocking(() -> {
            conversationService.delete(userId, conversationId);
            return ApiResponse.<Void>ok(null, requestId);
        });
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }
}
