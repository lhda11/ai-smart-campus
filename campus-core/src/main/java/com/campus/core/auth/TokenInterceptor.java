package com.campus.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 验证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** 跳过认证的路径 */
    private static final String[] SKIP_PATHS = {
            "/api/auth/login",   // 登录接口开放
            "/doc.html",         // Knife4j
            "/v3/api-docs",      // Swagger
            "/swagger-resources",
            "/webjars/",
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 跳过不需要认证的路径
        for (String skip : SKIP_PATHS) {
            if (path.startsWith(skip)) {
                return true;
            }
        }

        // OPTIONS 请求直接放行 (CORS 预检)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从 Header 中获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            log.warn("Token missing for path: {}", path);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}");
            return false;
        }

        // 去除 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证 Token
        try {
            UserContext.UserInfo userInfo = jwtUtil.getUserInfo(token);
            UserContext.set(userInfo);
            return true;
        } catch (Exception e) {
            log.warn("Token invalid for path: {} - {}", path, e.getMessage());
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
