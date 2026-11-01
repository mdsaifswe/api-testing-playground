package com.playground.api.security;

import com.playground.api.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitingService {

    @Value("${playground.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${playground.rate-limit.execute-per-minute:30}")
    private int executePerMinute;

    @Value("${playground.rate-limit.workspace-create-per-minute:10}")
    private int workspaceCreatePerMinute;

    @Value("${playground.rate-limit.api-per-minute:120}")
    private int apiPerMinute;

    private final ConcurrentHashMap<String, SimpleTokenBucket> buckets = new ConcurrentHashMap<>();

    public void checkExecuteRateLimit(String clientKey) {
        if (!enabled) return;
        String key = "exec:" + clientKey;
        SimpleTokenBucket bucket = buckets.computeIfAbsent(key, k -> new SimpleTokenBucket(executePerMinute, executePerMinute / 60.0));
        if (!bucket.tryConsume()) {
            int retryAfter = bucket.getSecondsUntilAvailable();
            log.warn("Rate limit exceeded for outbound request execution: key={}", clientKey);
            throw new RateLimitExceededException("Outbound request execution rate limit exceeded. Please wait before retrying.", retryAfter);
        }
    }

    public void checkWorkspaceCreateRateLimit(String clientIp) {
        if (!enabled) return;
        String key = "ws_create:" + clientIp;
        SimpleTokenBucket bucket = buckets.computeIfAbsent(key, k -> new SimpleTokenBucket(workspaceCreatePerMinute, workspaceCreatePerMinute / 60.0));
        if (!bucket.tryConsume()) {
            int retryAfter = bucket.getSecondsUntilAvailable();
            log.warn("Rate limit exceeded for workspace creation: ip={}", clientIp);
            throw new RateLimitExceededException("Workspace creation rate limit exceeded. Please wait before retrying.", retryAfter);
        }
    }

    public void checkGeneralApiRateLimit(String clientKey) {
        if (!enabled) return;
        String key = "api:" + clientKey;
        SimpleTokenBucket bucket = buckets.computeIfAbsent(key, k -> new SimpleTokenBucket(apiPerMinute, apiPerMinute / 60.0));
        if (!bucket.tryConsume()) {
            int retryAfter = bucket.getSecondsUntilAvailable();
            log.warn("General API rate limit exceeded: key={}", clientKey);
            throw new RateLimitExceededException("API request rate limit exceeded. Please wait before retrying.", retryAfter);
        }
    }

    /**
     * Clear all in-memory rate-limit buckets (useful for automated testing)
     */
    public void reset() {
        buckets.clear();
    }

    static class SimpleTokenBucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;

        SimpleTokenBucket(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized int getSecondsUntilAvailable() {
            refill();
            if (tokens >= 1.0) {
                return 0;
            }
            double needed = 1.0 - tokens;
            return Math.max(1, (int) Math.ceil(needed / refillRatePerSecond));
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastRefillTimestamp;
            if (elapsedMs > 0) {
                double tokensToAdd = (elapsedMs / 1000.0) * refillRatePerSecond;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }
}
