package edu.sezg534.codereview.guardrail;

import edu.sezg534.codereview.model.ReviewRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class InputGuardrail {

    private static final Pattern PROMPT_INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?(previous|prior)\\s+instructions|you\\s+are\\s+no\\s+longer|disregard\\s+all|override\\s+system\\s+prompt|hacked\\s+by)",
            Pattern.CASE_INSENSITIVE
    );

    public record GuardrailResult(boolean passed, boolean requiresClarification, String message, boolean hasPromptInjection) {}

    public GuardrailResult evaluate(ReviewRequest request) {
        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            return new GuardrailResult(
                    false,
                    true,
                    "The provided code input is empty. Please provide a valid source code snippet or git diff for review.",
                    false
            );
        }

        String code = request.getCode().trim();

        // Check for binary / unprintable characters (corrupted stream)
        long unprintableCount = code.chars()
                .filter(ch -> (ch < 32 && ch != '\t' && ch != '\n' && ch != '\r') || ch == 127)
                .count();

        if (unprintableCount > 2 || ((double) unprintableCount / code.length()) > 0.05) {
            return new GuardrailResult(
                    false,
                    true,
                    "Input appears to be binary, unparseable, or corrupted. Please supply valid UTF-8 source code.",
                    false
            );
        }

        // Check for pure gibberish symbols without keywords or alphanumerics
        long alphaNumericCount = code.chars().filter(Character::isLetterOrDigit).count();
        if (code.length() > 10 && ((double) alphaNumericCount / code.length()) < 0.20) {
            return new GuardrailResult(
                    false,
                    true,
                    "Input contains insufficient syntactic structure or alphanumeric code tokens. Please supply valid source code.",
                    false
            );
        }

        // Check for prompt injection attempts
        boolean injectionDetected = PROMPT_INJECTION_PATTERN.matcher(code).find();

        return new GuardrailResult(true, false, null, injectionDetected);
    }
}
