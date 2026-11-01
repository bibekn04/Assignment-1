package edu.sezg534.ratelimiter.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper to construct standard JSON error representations with key masking.
 */
public final class ApiErrorHandler {

    private ApiErrorHandler() {
    }

    public static Map<String, Object> unauthorized(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Unauthorized");
        body.put("message", message);
        return body;
    }

    public static Map<String, Object> rateLimitExceeded(long retryAfterSeconds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded");
        body.put("retryAfterSeconds", retryAfterSeconds);
        return body;
    }
}
