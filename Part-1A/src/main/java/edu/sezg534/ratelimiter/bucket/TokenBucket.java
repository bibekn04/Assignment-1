package edu.sezg534.ratelimiter.bucket;

import edu.sezg534.ratelimiter.limiter.Decision;

import java.time.Clock;

/**
 * Pure Token Bucket algorithm core.
 * NOTE: Initial stub for spec-derived test verification.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillRate;
    private final Clock clock;

    public TokenBucket(double capacity, double refillRate, Clock clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
    }

    public synchronized Decision tryConsume() {
        // Stub returning reject so tests fail on assertions initially
        return Decision.reject((long) capacity, 0, clock.instant().getEpochSecond(), 1);
    }

    public double getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }
}
