package edu.sezg534.ratelimiter.unit;

import edu.sezg534.ratelimiter.bucket.TokenBucket;
import edu.sezg534.ratelimiter.config.TierProperties.TierConfig;
import edu.sezg534.ratelimiter.store.InMemoryBucketStore;
import edu.sezg534.ratelimiter.support.AcceptanceCriterion;
import edu.sezg534.ratelimiter.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBucketStoreTest {

    private InMemoryBucketStore store;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-01T12:00:00Z"));
        store = new InMemoryBucketStore(clock);
    }

    @Test
    @DisplayName("AC-06: Buckets are isolated per client: exhausting client A does not affect client B")
    @AcceptanceCriterion("AC-06")
    void testClientBucketIsolation() {
        TierConfig tier = new TierConfig(2, 1.0);

        TokenBucket bucketA = store.getOrCreate("client_A", tier);
        TokenBucket bucketB = store.getOrCreate("client_B", tier);

        assertThat(bucketA).isNotSameAs(bucketB);

        // Exhaust client A
        bucketA.tryConsume();
        bucketA.tryConsume();

        // Client B is untouched
        TokenBucket retrievedB = store.getOrCreate("client_B", tier);
        assertThat(retrievedB.tryConsume().allowed()).isTrue();
    }

    @Test
    @DisplayName("AC-13: Store handles concurrent client bucket creation thread-safely")
    @AcceptanceCriterion("AC-13")
    void testStoreConcurrentBucketAccess() throws InterruptedException {
        TierConfig tier = new TierConfig(10, 1.0);
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startBarrier = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String clientId = "client_" + (i % 5);
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    store.getOrCreate(clientId, tier);
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

        assertThat(store.size()).isEqualTo(5);
    }
}
