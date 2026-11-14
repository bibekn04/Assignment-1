package edu.sezg534.codereview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CodeReviewContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Contract Test: Health check endpoint returns status UP")
    void testHealthEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("UP", json.path("status").asText());
        assertEquals("Semantic Code Review & Refactoring Assistant", json.path("service").asText());
    }

    @Test
    @DisplayName("Contract Test: /api/v1/review satisfies Output Schema Contract (AC-S01 to AC-S04)")
    void testReviewContractConformance() throws Exception {
        String payload = """
                {
                  "code": "public class Calc { public int add(int a, int b) { return a + b; } }",
                  "intent": "BOTH",
                  "language": "java",
                  "context": "Basic arithmetic utility"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        // AC-S02: Required fields
        assertTrue(json.hasNonNull("intent"), "intent must be present");
        assertTrue(json.hasNonNull("summary"), "summary must be present");
        assertTrue(json.hasNonNull("overall_quality"), "overall_quality must be present");
        assertTrue(json.has("findings"), "findings must be present");
        assertTrue(json.has("refactoring_suggestions"), "refactoring_suggestions must be present");
        assertTrue(json.has("clarification_needed"), "clarification_needed must be present");

        // AC-S03: Enum constraints
        String intent = json.path("intent").asText();
        assertTrue(intent.equals("REVIEW") || intent.equals("REFACTOR") || intent.equals("BOTH"));

        String quality = json.path("overall_quality").asText();
        assertTrue(quality.matches("POOR|NEEDS_IMPROVEMENT|ACCEPTABLE|GOOD|EXCELLENT"));

        // AC-S04: List types
        assertTrue(json.path("findings").isArray());
        assertTrue(json.path("refactoring_suggestions").isArray());
    }

    @Test
    @DisplayName("Contract Test: Empty code triggers clarification contract response")
    void testClarificationContract() throws Exception {
        String payload = """
                {
                  "code": "   ",
                  "intent": "REVIEW"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.path("clarification_needed").asBoolean());
        assertFalse(json.path("clarification_message").asText().isBlank());
    }
}
