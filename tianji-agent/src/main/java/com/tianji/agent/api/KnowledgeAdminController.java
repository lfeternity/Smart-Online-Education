package com.tianji.agent.api;

import com.tianji.agent.application.KnowledgeService;
import com.tianji.agent.application.KnowledgeService.KnowledgeInput;
import com.tianji.agent.application.IngestionJobService;
import com.tianji.agent.security.AgentAuthorization;
import com.tianji.agent.domain.KnowledgeDocumentEntity;
import com.tianji.agent.domain.IngestionJobEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/knowledge/documents")
public class KnowledgeAdminController {
    private final KnowledgeService service;
    private final AgentAuthorization authorization;
    private final IngestionJobService jobs;

    public KnowledgeAdminController(KnowledgeService service, AgentAuthorization authorization,
                                    IngestionJobService jobs) {
        this.service = service; this.authorization = authorization; this.jobs = jobs;
    }

    @PostMapping
    public Mono<ApiResponse<KnowledgeDocumentEntity>> create(@RequestHeader("user-info") Long userId,
                                                              @RequestHeader(value = "role-info", required = false) Long roleId,
                                                              @Valid @RequestBody KnowledgeInput input) {
        authorization.requireKnowledgeUploader(userId, roleId);
        return blocking(() -> ApiResponse.ok(service.create(input)));
    }

    @GetMapping
    public Mono<ApiResponse<List<KnowledgeDocumentEntity>>> list(@RequestHeader("user-info") Long userId,
                                                                  @RequestHeader(value = "role-info", required = false) Long roleId) {
        authorization.requireKnowledgeUploader(userId, roleId);
        return blocking(() -> ApiResponse.ok(service.list()));
    }

    @PostMapping("/{id}:publish")
    public Mono<ApiResponse<IngestionJobEntity>> publish(@RequestHeader("user-info") Long userId,
                                                          @RequestHeader(value = "role-info", required = false) Long roleId,
                                                          @PathVariable String id) {
        requireAdmin(userId, roleId);
        return blocking(() -> ApiResponse.ok(jobs.enqueue(id, userId)));
    }

    @PostMapping("/{id}:reindex")
    public Mono<ApiResponse<IngestionJobEntity>> reindex(@RequestHeader("user-info") Long userId,
                                                          @RequestHeader(value = "role-info", required = false) Long roleId,
                                                          @PathVariable String id) {
        requireAdmin(userId, roleId);
        return blocking(() -> {
            KnowledgeDocumentEntity version = service.createNextVersion(id);
            return ApiResponse.ok(jobs.enqueue(version.getId(), userId));
        });
    }

    private void requireAdmin(Long userId, Long roleId) {
        authorization.requireAdmin(userId, roleId, "需要知识管理员权限");
    }
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }
}
