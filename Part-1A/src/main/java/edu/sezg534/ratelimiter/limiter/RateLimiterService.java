package edu.sezg534.ratelimiter.limiter;

import edu.sezg534.ratelimiter.config.TierProperties;
import edu.sezg534.ratelimiter.store.BucketStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service orchestrating API key authentication, tier resolution, and token consumption.
 */
@Service
public class RateLimiterService {

    private final TierProperties tierProperties;
    private final BucketStore bucketStore;

    public RateLimiterService(TierProperties tierProperties, BucketStore bucketStore) {
        this.tierProperties = tierProperties;
        this.bucketStore = bucketStore;
    }

    public Optional<Decision> evaluate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String tierName = tierProperties.getClientTiers().get(apiKey);
        if (tierName == null) {
            return Optional.empty();
        }
        var tierConfig = tierProperties.getTiers().get(tierName);
        if (tierConfig == null) {
            return Optional.empty();
        }
        var bucket = bucketStore.getOrCreate(apiKey, tierConfig);
        return Optional.of(bucket.tryConsume());
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return apiKey.substring(0, Math.min(4, apiKey.length())) + "****";
    }
}
