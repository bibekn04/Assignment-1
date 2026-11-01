package edu.sezg534.ratelimiter.unit;

import edu.sezg534.ratelimiter.config.TierProperties;
import edu.sezg534.ratelimiter.limiter.Decision;
import edu.sezg534.ratelimiter.limiter.RateLimiterService;
import edu.sezg534.ratelimiter.store.InMemoryBucketStore;
import edu.sezg534.ratelimiter.support.AcceptanceCriterion;
import edu.sezg534.ratelimiter.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService service;
    private TierProperties properties;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-01T12:00:00Z"));
        properties = new TierProperties();
        properties.setTiers(Map.of(
                "free", new TierProperties.TierConfig(2, 1.0),
                "pro", new TierProperties.TierConfig(10, 5.0)
        ));
        properties.setClientTiers(Map.of(
                "key_free_123", "free",
                "key_pro_456", "pro"
        ));
        InMemoryBucketStore store = new InMemoryBucketStore(clock);
        service = new RateLimiterService(properties, store);
    }

    @Test
    @DisplayName("AC-01/AC-02/AC-03: Service evaluates decision accurately")
    @AcceptanceCriterion("AC-01")
    void testEvaluateReturnsCorrectDecision() {
        Optional<Decision> decision1 = service.evaluate("key_free_123");
        assertThat(decision1).isPresent();
        assertThat(decision1.get().limit()).isEqualTo(2);
    }

    @Test
    @DisplayName("AC-06: Service isolates different client keys")
    @AcceptanceCriterion("AC-06")
    void testServiceIsolatesClients() {
        Optional<Decision> dFree = service.evaluate("key_free_123");
        Optional<Decision> dPro = service.evaluate("key_pro_456");

        assertThat(dFree).isPresent();
        assertThat(dPro).isPresent();
        assertThat(dFree.get().limit()).isEqualTo(2);
        assertThat(dPro.get().limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("AC-07: Service enforces configured tier limits")
    @AcceptanceCriterion("AC-07")
    void testServiceEnforcesConfiguredTierLimits() {
        Optional<Decision> dPro = service.evaluate("key_pro_456");
        assertThat(dPro).isPresent();
        assertThat(dPro.get().limit()).isEqualTo(10);
    }

    @Test
    @DisplayName("AC-08: Decision record contains telemetry metrics")
    @AcceptanceCriterion("AC-08")
    void testDecisionContainsTelemetryMetrics() {
        Optional<Decision> d = service.evaluate("key_free_123");
        assertThat(d).isPresent();
        assertThat(d.get().resetEpochSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("AC-10: An unknown API key returns empty optional (unauthorized)")
    @AcceptanceCriterion("AC-10")
    void testUnknownApiKeyReturnsEmpty() {
        Optional<Decision> d = service.evaluate("key_unknown_999");
        assertThat(d).isEmpty();
    }
}
