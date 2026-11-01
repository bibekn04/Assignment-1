package edu.sezg534.ratelimiter.store;

import edu.sezg534.ratelimiter.bucket.TokenBucket;
import edu.sezg534.ratelimiter.config.TierProperties.TierConfig;

/**
 * Storage port interface for managing client token buckets.
 */
public interface BucketStore {
    TokenBucket getOrCreate(String clientId, TierConfig tierConfig);
    int size();
}
