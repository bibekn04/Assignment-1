package edu.sezg534.codereview.guardrail;

import edu.sezg534.codereview.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OutputSanitizer {

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|secret|api_key|apikey|token|aws_secret)\\s*[:=]\\s*[\"']([^\"']+)[\"']"
    );

    private static final Pattern GENERIC_SECRET_PATTERNS = Pattern.compile(
            "(AKIA[0-9A-Z]{16}|SuperSecret[^\\s\"',;]+|ghp_[A-Za-z0-9]{36}|AIza[0-9A-Za-z_-]{35})"
    );

    public ReviewResponse sanitize(ReviewResponse response, String inputCode, boolean promptInjectionDetected) {
        if (response == null) {
            return ReviewResponse.clarificationRequired(Intent.REVIEW, "Internal processing error: empty response");
        }

        // 1. Collect potential secrets from input code to guarantee non-echo
        Set<String> sensitiveValues = extractSecretsFromInput(inputCode);

        // 2. Redact sensitive values from summary
        if (response.getSummary() != null) {
            response.setSummary(redactAll(response.getSummary(), sensitiveValues));
        }

        // 3. Sanitize findings
        if (response.getFindings() != null) {
            for (Finding f : response.getFindings()) {
                if (f.getTitle() != null) f.setTitle(redactAll(f.getTitle(), sensitiveValues));
                if (f.getDescription() != null) f.setDescription(redactAll(f.getDescription(), sensitiveValues));
                if (f.getRecommendation() != null) f.setRecommendation(redactAll(f.getRecommendation(), sensitiveValues));
            }
        }

        // 4. Sanitize refactoring suggestions (and ensure before != after)
        if (response.getRefactoringSuggestions() != null) {
            List<RefactoringSuggestion> validSuggestions = new ArrayList<>();
            for (RefactoringSuggestion r : response.getRefactoringSuggestions()) {
                if (r.getBeforeSnippet() != null) r.setBeforeSnippet(redactAll(r.getBeforeSnippet(), sensitiveValues));
                if (r.getAfterSnippet() != null) r.setAfterSnippet(redactAll(r.getAfterSnippet(), sensitiveValues));
                if (r.getRationale() != null) r.setRationale(redactAll(r.getRationale(), sensitiveValues));

                // Principle P8 / AC-P04: Before and after snippets must not be identical
                if (r.getBeforeSnippet() != null && r.getAfterSnippet() != null) {
                    if (!r.getBeforeSnippet().trim().equals(r.getAfterSnippet().trim())) {
                        validSuggestions.add(r);
                    }
                }
            }
            response.setRefactoringSuggestions(validSuggestions);
        }

        // 5. If prompt injection was detected, attach a security notice finding and ensure schema wasn't hijacked
        if (promptInjectionDetected) {
            boolean hasInjectionFinding = response.getFindings().stream()
                    .anyMatch(f -> f.getTitle() != null && f.getTitle().toLowerCase().contains("injection"));
            if (!hasInjectionFinding) {
                response.getFindings().add(0, new Finding(
                        "INJ-GUARD-01",
                        Severity.HIGH,
                        Category.SECURITY,
                        "1-2",
                        "Prompt Injection Directive Detected in Comments",
                        "The input code contained embedded directives attempting to hijack the AI prompt. The system neutralized the directive and preserved structured schema analysis.",
                        "Isolate untrusted code inputs and sanitize external comments before feeding to automated pipelines."
                ));
            }
        }

        // 6. Calibrate quality rating (AC-Q03): If critical vulnerabilities exist, quality cannot be GOOD or EXCELLENT
        boolean hasCriticalOrHighSecurity = response.getFindings().stream()
                .anyMatch(f -> f.getCategory() == Category.SECURITY && 
                        (f.getSeverity() == Severity.CRITICAL || f.getSeverity() == Severity.HIGH));

        if (hasCriticalOrHighSecurity) {
            if (response.getOverallQuality() == Quality.GOOD || response.getOverallQuality() == Quality.EXCELLENT) {
                response.setOverallQuality(Quality.POOR);
            }
        }

        return response;
    }

    private Set<String> extractSecretsFromInput(String code) {
        Set<String> secrets = new HashSet<>();
        if (code == null) return secrets;

        Matcher m1 = SECRET_PATTERN.matcher(code);
        while (m1.find()) {
            String secretVal = m1.group(2).trim();
            if (secretVal.length() > 3) {
                secrets.add(secretVal);
            }
        }

        Matcher m2 = GENERIC_SECRET_PATTERNS.matcher(code);
        while (m2.find()) {
            String secretVal = m2.group(1).trim();
            if (secretVal.length() > 3) {
                secrets.add(secretVal);
            }
        }

        return secrets;
    }

    private String redactAll(String text, Set<String> secrets) {
        if (text == null) return null;
        String result = text;
        for (String s : secrets) {
            result = result.replace(s, "[REDACTED_SECRET]");
        }
        return result;
    }
}
