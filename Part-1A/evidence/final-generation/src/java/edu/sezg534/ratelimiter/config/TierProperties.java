package edu.sezg534.ratelimiter.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for tiers and client key mappings.
 */
@ConfigurationProperties(prefix = "ratelimiter")
@Validated
public class TierProperties {

    @NotEmpty(message = "Tiers map must not be empty")
    private Map<String, TierConfig> tiers = new HashMap<>();

    @NotNull(message = "Client tiers map must not be null")
    private Map<String, String> clientTiers = new HashMap<>();

    public Map<String, TierConfig> getTiers() {
        return Collections.unmodifiableMap(tiers);
    }

    public void setTiers(Map<String, TierConfig> tiers) {
        this.tiers = (tiers != null) ? new HashMap<>(tiers) : new HashMap<>();
    }

    public Map<String, String> getClientTiers() {
        return Collections.unmodifiableMap(clientTiers);
    }

    public void setClientTiers(Map<String, String> clientTiers) {
        this.clientTiers = (clientTiers != null) ? new HashMap<>(clientTiers) : new HashMap<>();
    }

    public static class TierConfig {
        @Min(value = 1, message = "Capacity must be greater than or equal to 1")
        private int capacity;

        @DecimalMin(value = "0.000001", message = "Refill rate must be strictly positive")
        private double refillRate;

        public TierConfig() {
        }

        public TierConfig(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public double getRefillRate() {
            return refillRate;
        }

        public void setRefillRate(double refillRate) {
            this.refillRate = refillRate;
        }
    }
}
