package edu.sezg534.ratelimiter.unit;

import edu.sezg534.ratelimiter.bucket.TokenBucket;
import edu.sezg534.ratelimiter.limiter.Decision;
import edu.sezg534.ratelimiter.support.AcceptanceCriterion;
import edu.sezg534.ratelimiter.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketTest {

    private MutableClock clock;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-11-01T12:00:00Z");
        clock = new MutableClock(baseTime);
    }

    @Test
    @DisplayName("AC-01: A new client with capacity C can make C requests at t=0, and all return 200")
    @AcceptanceCriterion("AC-01")
    void testCapacityBurstAllowedAtSameInstant() {
        TokenBucket bucket = new TokenBucket(5, 1.0, clock);

        for (int i = 0; i < 5; i++) {
            Decision decision = bucket.tryConsume();
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.remaining()).isEqualTo(4 - i);
        }
    }

    @Test
    @DisplayName("AC-02: Request C+1 at that same instant returns 429")
    @AcceptanceCriterion("AC-02")
    void testExcessRequestReturns429() {
        TokenBucket bucket = new TokenBucket(3, 1.0, clock);

        for (int i = 0; i < 3; i++) {
            assertThat(bucket.tryConsume().allowed()).isTrue();
        }

        Decision rejected = bucket.tryConsume();
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.remaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("AC-03: A 429 response includes Retry-After = ceil(seconds until 1 token is available) >= 1")
    @AcceptanceCriterion("AC-03")
    void testRetryAfterHeaderCalculation() {
        TokenBucket bucket = new TokenBucket(2, 0.5, clock); // 1 token every 2 seconds

        bucket.tryConsume();
        bucket.tryConsume();

        Decision decision = bucket.tryConsume();
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("AC-04: After the clock advances by 1/refill_rate s, exactly one more request succeeds")
    @AcceptanceCriterion("AC-04")
    void testRefillAllowsOneMoreRequestAfterRefillPeriod() {
        TokenBucket bucket = new TokenBucket(2, 2.0, clock); // 2 tokens/sec, 1 token every 0.5s

        // Exhaust bucket
        bucket.tryConsume();
        bucket.tryConsume();
        assertThat(bucket.tryConsume().allowed()).isFalse();

        // Advance by 500ms (1/refill_rate)
        clock.advance(Duration.ofMillis(500));

        Decision decisionAfterRefill = bucket.tryConsume();
        assertThat(decisionAfterRefill.allowed()).isTrue();

        // Immediate next should be rejected
        assertThat(bucket.tryConsume().allowed()).isFalse();
    }

    @Test
    @DisplayName("AC-05: After any idle period, tokens never exceed capacity")
    @AcceptanceCriterion("AC-05")
    void testTokensNeverExceedCapacityAfterExtendedIdle() {
        TokenBucket bucket = new TokenBucket(5, 10.0, clock);

        // Idle for 1 hour
        clock.advance(Duration.ofHours(1));

        Decision first = bucket.tryConsume();
        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(4);
    }

    @Test
    @DisplayName("AC-13: With N > C concurrent requests against capacity C, exactly C succeed")
    @AcceptanceCriterion("AC-13")
    void testConcurrentRequestsEnforceExactCapacity() throws InterruptedException {
        int capacity = 10;
        int totalRequests = 50;
        TokenBucket bucket = new TokenBucket(capacity, 1.0, clock);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startBarrier = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    Decision d = bucket.tryConsume();
                    if (d.allowed()) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startBarrier.countDown();
        finishLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(capacity);
    }
}
