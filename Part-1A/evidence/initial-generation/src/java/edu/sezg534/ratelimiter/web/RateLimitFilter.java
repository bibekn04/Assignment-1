package edu.sezg534.ratelimiter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sezg534.ratelimiter.limiter.Decision;
import edu.sezg534.ratelimiter.limiter.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter intercepting API requests, checking rate limits, and appending standard rate-limit headers.
 * NOTE: Initial stub for test-first phase.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/health") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, ApiErrorHandler.unauthorized("Missing API key"));
            return;
        }

        Optional<Decision> decisionOpt = rateLimiterService.evaluate(apiKey);
        if (decisionOpt.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, ApiErrorHandler.unauthorized("Invalid API key"));
            return;
        }

        Decision decision = decisionOpt.get();
        response.setHeader(HEADER_LIMIT, String.valueOf(decision.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(decision.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(decision.resetEpochSeconds()));

        if (!decision.allowed()) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, ApiErrorHandler.rateLimitExceeded(decision.retryAfterSeconds()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
