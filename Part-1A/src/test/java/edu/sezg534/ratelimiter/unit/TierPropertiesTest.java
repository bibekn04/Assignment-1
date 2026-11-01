package edu.sezg534.ratelimiter.unit;

import edu.sezg534.ratelimiter.config.TierProperties;
import edu.sezg534.ratelimiter.support.AcceptanceCriterion;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TierPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("AC-07: Each tier enforces its own configured capacity and refill rate")
    @AcceptanceCriterion("AC-07")
    void testTiersConfigurationValues() {
        TierProperties props = new TierProperties();
        props.setTiers(Map.of(
                "free", new TierProperties.TierConfig(5, 1.0),
                "pro", new TierProperties.TierConfig(20, 5.0)
        ));
        props.setClientTiers(Map.of("key_1", "free", "key_2", "pro"));

        assertThat(props.getTiers().get("free").getCapacity()).isEqualTo(5);
        assertThat(props.getTiers().get("free").getRefillRate()).isEqualTo(1.0);
        assertThat(props.getTiers().get("pro").getCapacity()).isEqualTo(20);
        assertThat(props.getTiers().get("pro").getRefillRate()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("AC-11: Invalid config (capacity <= 0, refill_rate <= 0) fails validation")
    @AcceptanceCriterion("AC-11")
    void testInvalidConfigValidationFails() {
        TierProperties.TierConfig invalidConfig = new TierProperties.TierConfig(0, -1.0);
        var violations = validator.validate(invalidConfig);
        assertThat(violations).isNotEmpty();
    }
}
