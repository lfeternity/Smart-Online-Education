package com.tianji.gateway.filter;

import com.tianji.authsdk.gateway.util.AuthUtil;
import com.tianji.common.domain.R;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.gateway.config.AuthProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.tianji.auth.common.constants.JwtConstants.AUTHORIZATION_HEADER;
import static com.tianji.auth.common.constants.JwtConstants.USER_HEADER;

@Component
public class AccountAuthFilter implements GlobalFilter, Ordered {
    private static final String ROLE_HEADER = "role-info";

    private final AuthUtil authUtil;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public AccountAuthFilter(AuthUtil authUtil, AuthProperties authProperties) {
        this.authUtil = authUtil;
        this.authProperties = authProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Identity headers are internal and trusted. Never relay client-supplied values.
        exchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.remove(USER_HEADER);
                    headers.remove(ROLE_HEADER);
                }))
                .build();
        // 1.获取请求request信息
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethodValue();
        String path = request.getPath().toString();
        String antPath = method + ":" + path;

        // 2.判断是否是无需登录的路径
        if(isExcludePath(antPath)){
            // 直接放行
            return chain.filter(exchange);
        }

        // 3.尝试获取用户信息
        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String token = authHeaders == null ? "" : authHeaders.get(0);
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        R<LoginUserDTO> r = authUtil.parseToken(token);

        // 4.如果用户是登录状态，尝试更新请求头，传递用户信息
        if(r.success()){
            exchange = exchange.mutate()
                    .request(builder -> builder.headers(headers ->
                    {
                        headers.set(USER_HEADER, r.getData().getUserId().toString());
                        if (r.getData().getRoleId() != null) {
                            headers.set(ROLE_HEADER, r.getData().getRoleId().toString());
                        }
                    }))
                    .build();
        }

        // 5.校验权限
        authUtil.checkAuth(antPath, r);

        // 6.放行
        return chain.filter(exchange);
    }

    private boolean isExcludePath(String antPath) {
        for (String pathPattern : authProperties.getExcludePath()) {
            if(antPathMatcher.match(pathPattern, antPath)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}
