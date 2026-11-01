package edu.sezg534.ratelimiter.store;

import edu.sezg534.ratelimiter.bucket.TokenBucket;
import edu.sezg534.ratelimiter.config.TierProperties.TierConfig;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe adapter for BucketStore.
 */
@Component
public class InMemoryBucketStore implements BucketStore {

    private final Clock clock;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryBucketStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public TokenBucket getOrCreate(String clientId, TierConfig tierConfig) {
        return buckets.computeIfAbsent(clientId, id ->
                new TokenBucket(tierConfig.getCapacity(), tierConfig.getRefillRate(), clock));
    }

    @Override
    public int size() {
        return buckets.size();
    }
}
