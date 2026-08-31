package com.tianji.agent.api;

import com.tianji.agent.api.ConversationDtos.PendingActionResponse;
import com.tianji.agent.application.PendingActionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {

    private final PendingActionService service;

    public ActionController(PendingActionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/confirm")
    public Mono<ApiResponse<PendingActionResponse>> confirm(@RequestHeader("user-info") Long userId,
                                                            @RequestHeader(value = "requestId", required = false) String requestId,
                                                            @PathVariable String id) {
        String traceId = requestId == null || requestId.isBlank() ? RequestIds.create() : requestId;
        return Mono.fromCallable(() -> ApiResponse.ok(service.confirm(userId, traceId, id), traceId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/{id}/cancel")
    public Mono<ApiResponse<PendingActionResponse>> cancel(@RequestHeader("user-info") Long userId,
                                                           @RequestHeader(value = "requestId", required = false) String requestId,
                                                           @PathVariable String id) {
        String traceId = requestId == null || requestId.isBlank() ? RequestIds.create() : requestId;
        return Mono.fromCallable(() -> ApiResponse.ok(service.cancel(userId, id), traceId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
