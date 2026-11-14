package edu.sezg534.codereview;

import edu.sezg534.codereview.guardrail.OutputSanitizer;
import edu.sezg534.codereview.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OutputSanitizerTest {

    private OutputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new OutputSanitizer();
    }

    @Test
    @DisplayName("AC-P03: Verifies secret non-echo in summary and findings")
    void testSecretRedaction() {
        String inputCode = "private String dbPass = \"SuperSecretP@ssword2026!\";";
        ReviewResponse response = new ReviewResponse();
        response.setSummary("Database password was set to SuperSecretP@ssword2026!");
        response.setOverallQuality(Quality.ACCEPTABLE);
        
        List<Finding> findings = new ArrayList<>();
        findings.add(new Finding(
                "F1",
                Severity.CRITICAL,
                Category.SECURITY,
                "1",
                "Password exposed: SuperSecretP@ssword2026!",
                "Do not commit SuperSecretP@ssword2026! in code",
                "Remove SuperSecretP@ssword2026!"
        ));
        response.setFindings(findings);

        ReviewResponse sanitized = sanitizer.sanitize(response, inputCode, false);

        assertFalse(sanitized.getSummary().contains("SuperSecretP@ssword2026!"));
        assertTrue(sanitized.getSummary().contains("[REDACTED_SECRET]"));
        assertFalse(sanitized.getFindings().get(0).getTitle().contains("SuperSecretP@ssword2026!"));
        assertFalse(sanitized.getFindings().get(0).getDescription().contains("SuperSecretP@ssword2026!"));
        assertFalse(sanitized.getFindings().get(0).getRecommendation().contains("SuperSecretP@ssword2026!"));
    }

    @Test
    @DisplayName("AC-P04: Refactoring suggestion before and after must not be identical")
    void testIdenticalRefactoringFiltered() {
        ReviewResponse response = new ReviewResponse();
        response.setSummary("Review with bad refactoring");
        response.setOverallQuality(Quality.GOOD);

        List<RefactoringSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new RefactoringSuggestion("R1", "Identical", "No change", "int a = 1;", "int a = 1;"));
        suggestions.add(new RefactoringSuggestion("R2", "Valid", "Better", "int a = 1;", "final int a = 1;"));
        response.setRefactoringSuggestions(suggestions);

        ReviewResponse sanitized = sanitizer.sanitize(response, "int a = 1;", false);

        assertEquals(1, sanitized.getRefactoringSuggestions().size());
        assertEquals("R2", sanitized.getRefactoringSuggestions().get(0).getId());
    }

    @Test
    @DisplayName("AC-Q03: Quality calibration downgrades when critical security flaws exist")
    void testQualityCalibrationOnCriticalSecurity() {
        ReviewResponse response = new ReviewResponse();
        response.setSummary("Code has SQL injection");
        response.setOverallQuality(Quality.EXCELLENT); // Erroneously high

        List<Finding> findings = new ArrayList<>();
        findings.add(new Finding("F1", Severity.CRITICAL, Category.SECURITY, "1", "SQLi", "SQL injection", "Fix"));
        response.setFindings(findings);

        ReviewResponse sanitized = sanitizer.sanitize(response, "SELECT * FROM users", false);

        assertEquals(Quality.POOR, sanitized.getOverallQuality());
    }
}
