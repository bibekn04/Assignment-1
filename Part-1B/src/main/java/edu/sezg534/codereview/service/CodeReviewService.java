package edu.sezg534.codereview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sezg534.codereview.guardrail.InputGuardrail;
import edu.sezg534.codereview.guardrail.OutputSanitizer;
import edu.sezg534.codereview.llm.GeminiLlmClient;
import edu.sezg534.codereview.llm.LocalDeterministicLlmEngine;
import edu.sezg534.codereview.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CodeReviewService {

    private static final Logger logger = LoggerFactory.getLogger(CodeReviewService.class);

    private final InputGuardrail inputGuardrail;
    private final OutputSanitizer outputSanitizer;
    private final GeminiLlmClient geminiLlmClient;
    private final LocalDeterministicLlmEngine localLlmEngine;
    private final ObjectMapper objectMapper;

    public CodeReviewService(InputGuardrail inputGuardrail,
                             OutputSanitizer outputSanitizer,
                             GeminiLlmClient geminiLlmClient,
                             LocalDeterministicLlmEngine localLlmEngine) {
        this.inputGuardrail = inputGuardrail;
        this.outputSanitizer = outputSanitizer;
        this.geminiLlmClient = geminiLlmClient;
        this.localLlmEngine = localLlmEngine;
        this.objectMapper = new ObjectMapper();
    }

    public ReviewResponse processReview(ReviewRequest request) {
        // 1. Layer A: Input Guardrail Check
        InputGuardrail.GuardrailResult guardrailResult = inputGuardrail.evaluate(request);
        if (guardrailResult.requiresClarification()) {
            Intent reqIntent = request != null && request.getIntent() != null ? request.getIntent() : Intent.REVIEW;
            return ReviewResponse.clarificationRequired(reqIntent, guardrailResult.message());
        }

        // 2. Layer B & C: LLM Execution with Fallback
        String rawLlmOutput = null;
        if (geminiLlmClient.isLiveApi()) {
            try {
                rawLlmOutput = geminiLlmClient.generateReview(request);
            } catch (Exception e) {
                logger.warn("Live Gemini API call failed ({}), falling back to local deterministic engine", e.getMessage());
                rawLlmOutput = invokeLocalEngine(request);
            }
        } else {
            rawLlmOutput = invokeLocalEngine(request);
        }

        // 3. Layer D: Output Parsing & Schema Repair
        ReviewResponse parsedResponse = parseAndEnforceSchema(rawLlmOutput, request);

        // 4. Layer E: Output Sanitization & Security Redaction
        String inputCode = request != null ? request.getCode() : "";
        return outputSanitizer.sanitize(parsedResponse, inputCode, guardrailResult.hasPromptInjection());
    }

    private String invokeLocalEngine(ReviewRequest request) {
        try {
            return localLlmEngine.generateReview(request);
        } catch (Exception ex) {
            logger.error("Local engine error: {}", ex.getMessage());
            return "{}";
        }
    }

    private ReviewResponse parseAndEnforceSchema(String rawJson, ReviewRequest request) {
        if (rawJson == null || rawJson.isBlank()) {
            return fallbackEmptyResponse(request);
        }

        // Strip markdown code fences if model wrapped in ```json ... ```
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            ReviewResponse response = objectMapper.readValue(cleaned, ReviewResponse.class);
            // Invariant enforcement: ensure required fields are not null
            if (response.getIntent() == null) {
                response.setIntent(request.getIntent() != null ? request.getIntent() : Intent.BOTH);
            }
            if (response.getOverallQuality() == null) {
                response.setOverallQuality(Quality.ACCEPTABLE);
            }
            if (response.getSummary() == null || response.getSummary().isBlank()) {
                response.setSummary("Code review completed.");
            }
            if (response.getFindings() == null) {
                response.setFindings(new ArrayList<>());
            }
            if (response.getRefactoringSuggestions() == null) {
                response.setRefactoringSuggestions(new ArrayList<>());
            }
            return response;
        } catch (Exception e) {
            logger.error("Failed to parse LLM JSON output: {}. Triggering schema repair fallback.", e.getMessage());
            return fallbackEmptyResponse(request);
        }
    }

    private ReviewResponse fallbackEmptyResponse(ReviewRequest request) {
        ReviewResponse fallback = new ReviewResponse();
        fallback.setIntent(request != null && request.getIntent() != null ? request.getIntent() : Intent.BOTH);
        fallback.setSummary("Review generated under schema repair fallback.");
        fallback.setOverallQuality(Quality.ACCEPTABLE);
        fallback.setFindings(new ArrayList<>());
        fallback.setRefactoringSuggestions(new ArrayList<>());
        fallback.setClarificationNeeded(false);
        return fallback;
    }
}
