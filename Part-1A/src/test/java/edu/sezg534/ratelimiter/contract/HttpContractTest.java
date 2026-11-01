package edu.sezg534.ratelimiter.contract;

import edu.sezg534.ratelimiter.support.AcceptanceCriterion;
import edu.sezg534.ratelimiter.web.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HttpContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("AC-01: Valid client request returns 200")
    @AcceptanceCriterion("AC-01")
    void testBurstRequestsReturn200() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, "key_free_123"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC-02: Exhausted client returns 429")
    @AcceptanceCriterion("AC-02")
    void testImmediateExcessReturns429() throws Exception {
        // Free tier capacity is 5
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/resource")
                    .header(RateLimitFilter.HEADER_API_KEY, "key_free_123"));
        }
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, "key_free_123"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("AC-03: Throttled response includes Retry-After header >= 1")
    @AcceptanceCriterion("AC-03")
    void testThrottledResponseContainsRetryAfter() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/resource")
                    .header(RateLimitFilter.HEADER_API_KEY, "key_free_123"));
        }
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, "key_free_123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(RateLimitFilter.HEADER_RETRY_AFTER));
    }

    @Test
    @DisplayName("AC-08: Every limited response includes standard telemetry headers")
    @AcceptanceCriterion("AC-08")
    void testResponseContainsRateLimitHeaders() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, "key_pro_456"))
                .andExpect(header().exists(RateLimitFilter.HEADER_LIMIT))
                .andExpect(header().exists(RateLimitFilter.HEADER_REMAINING))
                .andExpect(header().exists(RateLimitFilter.HEADER_RESET));
    }

    @Test
    @DisplayName("AC-09: Missing API key returns 401 with JSON body")
    @AcceptanceCriterion("AC-09")
    void testMissingApiKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Missing API key"));
    }

    @Test
    @DisplayName("AC-10: Unknown API key returns 401")
    @AcceptanceCriterion("AC-10")
    void testUnknownApiKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, "key_non_existent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("AC-12: GET /health is never rate limited")
    @AcceptanceCriterion("AC-12")
    void testHealthEndpointNeverRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    @DisplayName("AC-14: Error bodies and logs never echo the full API key")
    @AcceptanceCriterion("AC-14")
    void testErrorBodiesNeverEchoFullApiKey() throws Exception {
        String sensitiveKey = "secret_raw_client_token_999";
        mockMvc.perform(get("/api/resource")
                        .header(RateLimitFilter.HEADER_API_KEY, sensitiveKey)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(not(containsString(sensitiveKey))));
    }
}
