package com.tianji.agent.api;

import com.tianji.agent.api.ConversationDtos.FeedbackRequest;
import com.tianji.agent.application.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/messages")
public class FeedbackController {

    private final ConversationService conversationService;

    public FeedbackController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/{id}/feedback")
    public Mono<ApiResponse<Void>> feedback(
            @RequestHeader("user-info") Long userId,
            @RequestHeader(value = "requestId", required = false) String requestId,
            @PathVariable("id") String messageId,
            @Valid @RequestBody FeedbackRequest request
    ) {
        return Mono.fromCallable(() -> {
            conversationService.feedback(userId, messageId, request);
            return ApiResponse.<Void>ok(null, requestId);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
