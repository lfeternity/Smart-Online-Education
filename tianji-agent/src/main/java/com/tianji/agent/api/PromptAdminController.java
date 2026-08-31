package com.tianji.agent.api;

import com.tianji.agent.application.PromptService;
import com.tianji.agent.security.AgentAuthorization;
import com.tianji.agent.domain.PromptVersionEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/prompts")
public class PromptAdminController {
    public record CreatePromptRequest(String version, String content) {}
    private final PromptService service; private final AgentAuthorization authorization;
    public PromptAdminController(PromptService service, AgentAuthorization authorization) { this.service = service; this.authorization = authorization; }
    @GetMapping public Mono<ApiResponse<List<PromptVersionEntity>>> list(@RequestHeader("user-info") Long userId,
                                                                         @RequestHeader(value = "role-info", required = false) Long roleId) {
        admin(userId, roleId); return blocking(() -> ApiResponse.ok(service.list()));
    }
    @PostMapping public Mono<ApiResponse<PromptVersionEntity>> create(@RequestHeader("user-info") Long userId,
                                                                       @RequestHeader(value = "role-info", required = false) Long roleId,
                                                                       @RequestBody CreatePromptRequest request) {
        admin(userId, roleId); return blocking(() -> ApiResponse.ok(service.create(request.version(), request.content())));
    }
    @PostMapping("/{id}:publish") public Mono<ApiResponse<PromptVersionEntity>> publish(@RequestHeader("user-info") Long userId,
                                                                                         @RequestHeader(value = "role-info", required = false) Long roleId,
                                                                                         @PathVariable String id) {
        admin(userId, roleId); return blocking(() -> ApiResponse.ok(service.publish(id, userId)));
    }
    private void admin(Long userId, Long roleId) { authorization.requireAdmin(userId, roleId, "需要 Prompt 管理权限"); }
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> value) { return Mono.fromCallable(value).subscribeOn(Schedulers.boundedElastic()); }
}
