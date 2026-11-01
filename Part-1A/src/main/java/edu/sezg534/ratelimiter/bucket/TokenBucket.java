package edu.sezg534.ratelimiter.bucket;

import edu.sezg534.ratelimiter.limiter.Decision;

import java.time.Clock;
import java.time.Instant;

/**
 * Pure Token Bucket algorithm implementation.
 * Pure Java 17, no Spring dependencies, all time derived from injected java.time.Clock.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillRate;
    private final Clock clock;

    private double tokens;
    private Instant lastRefillTime;

    public TokenBucket(double capacity, double refillRate, Clock clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillTime = clock.instant();
    }

    /**
     * Attempts to consume 1 token from the bucket.
     * Evaluates in O(1) time and is thread-safe.
     */
    public synchronized Decision tryConsume() {
        refill();

        long capLong = (long) capacity;
        Instant now = clock.instant();

        if (tokens >= 1.0) {
            tokens -= 1.0;
            long remaining = (long) Math.floor(tokens);
            long resetEpochSeconds = calculateResetEpochSeconds(now);
            return Decision.allow(capLong, remaining, resetEpochSeconds);
        } else {
            long remaining = (long) Math.floor(tokens);
            long resetEpochSeconds = calculateResetEpochSeconds(now);
            long retryAfterSeconds = calculateRetryAfterSeconds();
            return Decision.reject(capLong, remaining, resetEpochSeconds, retryAfterSeconds);
        }
    }

    private void refill() {
        Instant now = clock.instant();
        if (now.isAfter(lastRefillTime)) {
            long millisElapsed = java.time.Duration.between(lastRefillTime, now).toMillis();
            if (millisElapsed > 0) {
                double secondsElapsed = millisElapsed / 1000.0;
                double generatedTokens = secondsElapsed * refillRate;
                tokens = Math.min(capacity, tokens + generatedTokens);
                lastRefillTime = now;
            }
        }
    }

    private long calculateResetEpochSeconds(Instant now) {
        if (tokens >= capacity) {
            return now.getEpochSecond();
        }
        double tokensNeeded = capacity - tokens;
        double secondsUntilFull = tokensNeeded / refillRate;
        return now.getEpochSecond() + (long) Math.ceil(secondsUntilFull);
    }

    private long calculateRetryAfterSeconds() {
        double tokensNeeded = 1.0 - tokens;
        if (tokensNeeded <= 0.0) {
            return 1;
        }
        long waitSec = (long) Math.ceil(tokensNeeded / refillRate);
        return Math.max(1, waitSec);
    }

    public double getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }

    public synchronized double getAvailableTokens() {
        refill();
        return tokens;
    }
}
