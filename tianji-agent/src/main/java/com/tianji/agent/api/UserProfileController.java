package com.tianji.agent.api;

import com.tianji.agent.application.UserProfileService;
import com.tianji.agent.application.UserProfileService.ProfileInput;
import com.tianji.agent.domain.UserProfileEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/profile")
public class UserProfileController {
    private final UserProfileService service;
    public UserProfileController(UserProfileService service) { this.service = service; }

    @GetMapping
    public Mono<ApiResponse<UserProfileEntity>> get(@RequestHeader("user-info") Long userId) {
        return blocking(() -> ApiResponse.ok(service.get(userId)));
    }

    @PutMapping
    public Mono<ApiResponse<UserProfileEntity>> save(@RequestHeader("user-info") Long userId,
                                                      @Valid @RequestBody ProfileInput input) {
        return blocking(() -> ApiResponse.ok(service.save(userId, input)));
    }

    @DeleteMapping
    public Mono<ApiResponse<Void>> delete(@RequestHeader("user-info") Long userId) {
        return blocking(() -> { service.delete(userId); return ApiResponse.<Void>ok(null); });
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> value) {
        return Mono.fromCallable(value).subscribeOn(Schedulers.boundedElastic());
    }
}
