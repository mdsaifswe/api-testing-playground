package com.playground.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.response.ApiErrorResponse;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.RateLimitExceededException;
import com.playground.api.exception.UnauthorizedException;
import com.playground.api.service.WorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceInterceptor implements HandlerInterceptor {

    public static final String WORKSPACE_KEY_HEADER = "X-Workspace-Key";

    private final WorkspaceService workspaceService;
    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Bypass public and documentation endpoints
        if (isPublicEndpoint(path, method)) {
            // Apply workspace creation rate limit to POST /api/v1/workspaces
            if ("/api/v1/workspaces".equals(path) && "POST".equalsIgnoreCase(method)) {
                try {
                    rateLimitingService.checkWorkspaceCreateRateLimit(resolveClientIp(request));
                } catch (RateLimitExceededException ex) {
                    sendRateLimitError(response, path, ex);
                    return false;
                }
            }
            return true;
        }

        // 2. Extract and validate X-Workspace-Key header
        String rawKey = request.getHeader(WORKSPACE_KEY_HEADER);
        if (rawKey == null || rawKey.trim().isEmpty()) {
            sendUnauthorizedError(response, path, "Missing required header: X-Workspace-Key. Please create or supply a valid workspace key.");
            return false;
        }

        WorkspaceEntity workspace;
        try {
            workspace = workspaceService.resolveWorkspace(rawKey);
        } catch (UnauthorizedException ex) {
            sendUnauthorizedError(response, path, ex.getMessage());
            return false;
        }

        // 3. Apply rate limits per workspace
        try {
            if ("/api/v1/execute".equals(path)) {
                rateLimitingService.checkExecuteRateLimit(String.valueOf(workspace.getId()));
            } else {
                rateLimitingService.checkGeneralApiRateLimit(String.valueOf(workspace.getId()));
            }
        } catch (RateLimitExceededException ex) {
            sendRateLimitError(response, path, ex);
            return false;
        }

        // 4. Set thread-local context
        WorkspaceContextHolder.set(workspace);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        WorkspaceContextHolder.clear();
    }

    private boolean isPublicEndpoint(String path, String method) {
        if (path == null) return false;
        if (path.equals("/api/v1/health") || path.startsWith("/api/v1/echo/")) {
            return true;
        }
        if (path.equals("/api/v1/workspaces") && "POST".equalsIgnoreCase(method)) {
            return true;
        }
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.equals("/error")) {
            return true;
        }
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private void sendUnauthorizedError(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        ApiErrorResponse error = ApiErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error("Unauthorized")
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private void sendRateLimitError(HttpServletResponse response, String path, RateLimitExceededException ex) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        response.setContentType("application/json");
        ApiErrorResponse error = ApiErrorResponse.builder()
                .status(429)
                .error("Too Many Requests")
                .message(ex.getMessage())
                .path(path)
                .timestamp(Instant.now())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
