package edu.sezg534.ratelimiter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected demo resource controller.
 */
@RestController
public class ResourceController {

    @GetMapping("/api/resource")
    public ResponseEntity<Map<String, String>> getResource() {
        return ResponseEntity.ok(Map.of("message", "Access granted"));
    }
}
