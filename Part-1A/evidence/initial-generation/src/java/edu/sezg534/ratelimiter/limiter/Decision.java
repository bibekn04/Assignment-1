package edu.sezg534.ratelimiter.limiter;

/**
 * Immutable record representing the result of a rate-limit decision.
 */
public record Decision(
        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSeconds,
        long retryAfterSeconds
) {
    public static Decision reject(long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds) {
        return new Decision(false, limit, remaining, resetEpochSeconds, Math.max(1, retryAfterSeconds));
    }

    public static Decision allow(long limit, long remaining, long resetEpochSeconds) {
        return new Decision(true, limit, remaining, resetEpochSeconds, 0);
    }
}
