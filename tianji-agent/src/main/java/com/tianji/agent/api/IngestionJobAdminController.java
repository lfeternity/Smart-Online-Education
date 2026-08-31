package com.tianji.agent.api;

import com.tianji.agent.application.IngestionJobService;
import com.tianji.agent.domain.IngestionJobEntity;
import com.tianji.agent.security.AgentAuthorization;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/admin/knowledge/jobs")
public class IngestionJobAdminController {
    private final IngestionJobService jobs;
    private final AgentAuthorization authorization;

    public IngestionJobAdminController(IngestionJobService jobs, AgentAuthorization authorization) {
        this.jobs = jobs;
        this.authorization = authorization;
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<IngestionJobEntity>> get(@RequestHeader("user-info") Long userId,
                                                      @RequestHeader(value = "role-info", required = false) Long roleId,
                                                      @PathVariable String id) {
        authorization.requireAdmin(userId, roleId, "需要知识管理员权限");
        return blocking(() -> ApiResponse.ok(jobs.get(id)));
    }

    @PostMapping("/{id}:retry")
    public Mono<ApiResponse<IngestionJobEntity>> retry(@RequestHeader("user-info") Long userId,
                                                        @RequestHeader(value = "role-info", required = false) Long roleId,
                                                        @PathVariable String id) {
        authorization.requireAdmin(userId, roleId, "需要知识管理员权限");
        return blocking(() -> ApiResponse.ok(jobs.retry(id)));
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }
}
