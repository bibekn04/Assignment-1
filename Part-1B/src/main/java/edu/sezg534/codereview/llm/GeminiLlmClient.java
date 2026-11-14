package edu.sezg534.codereview.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sezg534.codereview.model.ReviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLlmClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${codereview.llm.apiKey:}")
    private String apiKey;

    @Value("${codereview.llm.model:gemini-1.5-flash}")
    private String model;

    public GeminiLlmClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isLiveApi() {
        return apiKey != null && !apiKey.isBlank() && apiKey.startsWith("AIza");
    }

    @Override
    public String generateReview(ReviewRequest request) throws Exception {
        if (!isLiveApi()) {
            throw new IllegalStateException("Valid Gemini API Key not configured. Use LocalDeterministicLlmEngine.");
        }

        String prompt = buildPrompt(request);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", List.of(content));

        // Enforce JSON output format where supported
        Map<String, Object> genConfig = Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        );
        requestBodyMap.put("generationConfig", genConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBodyMap);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.error("Gemini API error status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API call failed with status: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new RuntimeException("Gemini returned empty completion");
        }

        return textNode.asText();
    }

    private String buildPrompt(ReviewRequest request) {
        return """
                You are a Principal Software Engineer and Security Auditor.
                Analyze the following %s code for intent %s.
                Context: %s

                CODE:
                %s

                Respond ONLY with a valid JSON object matching this schema:
                {
                  "intent": "%s",
                  "summary": "...",
                  "overall_quality": "POOR"|"NEEDS_IMPROVEMENT"|"ACCEPTABLE"|"GOOD"|"EXCELLENT",
                  "findings": [
                    {
                      "id": "FINDING-1",
                      "severity": "CRITICAL"|"HIGH"|"MEDIUM"|"LOW"|"INFO",
                      "category": "SECURITY"|"PERFORMANCE"|"STYLE"|"CORRECTNESS"|"MAINTAINABILITY"|"READABILITY",
                      "line_range": "...",
                      "title": "...",
                      "description": "...",
                      "recommendation": "..."
                    }
                  ],
                  "refactoring_suggestions": [
                    {
                      "id": "REFACTOR-1",
                      "title": "...",
                      "rationale": "...",
                      "before_snippet": "...",
                      "after_snippet": "..."
                    }
                  ],
                  "clarification_needed": false,
                  "clarification_message": null
                }
                Never echo secrets, passwords, or credentials verbatim; mask them as [REDACTED_SECRET].
                """.formatted(
                request.getLanguage(),
                request.getIntent(),
                request.getContext(),
                request.getCode(),
                request.getIntent()
        );
    }
}
