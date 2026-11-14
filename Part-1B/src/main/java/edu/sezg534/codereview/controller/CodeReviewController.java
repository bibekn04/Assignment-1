package edu.sezg534.codereview.controller;

import edu.sezg534.codereview.llm.GeminiLlmClient;
import edu.sezg534.codereview.model.ReviewRequest;
import edu.sezg534.codereview.model.ReviewResponse;
import edu.sezg534.codereview.service.CodeReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CodeReviewController {

    private final CodeReviewService reviewService;
    private final GeminiLlmClient geminiLlmClient;

    public CodeReviewController(CodeReviewService reviewService, GeminiLlmClient geminiLlmClient) {
        this.reviewService = reviewService;
        this.geminiLlmClient = geminiLlmClient;
    }

    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> reviewCode(@RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.processReview(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Semantic Code Review & Refactoring Assistant",
                "version", "1.0.0",
                "live_api_active", geminiLlmClient.isLiveApi()
        ));
    }
}
