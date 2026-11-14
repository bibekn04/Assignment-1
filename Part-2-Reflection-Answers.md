# ASSIGNMENT 1 — PART 2: ASSESSED REFLECTION QUESTIONS
**Course:** AI-Augmented Software Development Life Cycle (SE* ZG534)  
**Total Marks:** 5 (1 mark each)  
**Student Name / ID:** Akshaya Ganesan / 2026-SEZG534  
**GitHub Repository:** [Public Repository URL]  
**Demonstration Video Link:** [Google Drive / OneDrive Link]  

---

### Question 1: Specification Influence (1 mark)
**Prompt:** Identify one concrete decision in your implementation that was directly shaped by a Spec Kit artifact (constitution, specification, clarification, plan, or tasks). Explain the traceability from the artifact to the implemented decision.

**Response:**
A concrete implementation decision directly shaped by the Spec Kit was the implementation of the zero-tolerance **Secret Non-Echo Guardrail** (`OutputSanitizer.java`) in Part 1B. 
During the specification phase, Constitution Principle **P6** ("The response MUST NOT reproduce verbatim any secret value present in the input; Gate: Secret echo count = 0") and Specification criterion **AC-P03** explicitly prohibited the assistant from reflecting raw credentials committed in inspected source code. In the Clarification phase (`clarifications.md`, Question 3), we analyzed that LLMs naturally echo context strings (e.g., stating *"Password 'SuperSecretP@ssword2026!' is insecure"*). Rather than relying on non-deterministic system prompt adherence alone, Task **TASK-05** mandated an independent deterministic post-processing layer. This led directly to creating `OutputSanitizer.java`, which extracts credential regex patterns from the input and deterministically replaces all occurrences with `[REDACTED_SECRET]` across summaries, findings, and refactoring snippets before the JSON response leaves the service. This traceability from **Constitution P6 → Spec AC-P03 → Clarifications Q3 → TASK-05 → `OutputSanitizer.java`** prevented secret leakage in benchmark case `TC-07`.

---

### Question 2: Harness Value in Part 1A (1 mark)
**Prompt:** Describe one issue detected by the deterministic harness that would have been missed or less reliably detected by simply asking the coding agent to review its own output. State the relevant harness gate/evidence and how the issue was resolved.

**Response:**
In Part 1A (Tiered Rate Limiter Microservice), the deterministic harness detected a critical **concurrent race condition and time-window drift** under high multithreaded contention that self-review failed to catch. 
During development, the coding agent initially implemented token replenishment using a standard `long lastRefillTimestamp` with non-atomic arithmetic (`Math.min(capacity, tokens + delta)`). When self-reviewing the code, the LLM stated the logic was *"clean, thread-safe, and correct"*, exhibiting confirmation bias. 
However, Harness Gate **Gate 3 (Deterministic Concurrency & Contract Test)** executed 50 concurrent client threads requesting tokens simultaneously against `InMemoryRateLimiter`. The harness captured an assertion failure: 12 excess requests were permitted beyond the strict 100 req/min tier threshold because concurrent threads read a stale token count simultaneously before writing back. The structured failure evidence (`AssertionError: Expected 429 TOO_MANY_REQUESTS at request #101, but got 200 OK`) was fed back to the agent. The issue was resolved by replacing plain variables with `AtomicLong` and utilizing a synchronized CAS (Compare-And-Swap) refill loop and a mockable `MutableClock`, guaranteeing atomic token consumption and satisfying Gate 3 with zero test failures.

---

### Question 3: Evaluating Non-Determinism in Part 1B (1 mark)
**Prompt:** Choose one evaluation case where exact string matching would be inappropriate. Explain which schema constraint, invariant/property, and semantic/quality criterion you used instead, and why these checks were suitable.

**Response:**
Exact string matching is completely inappropriate for **TC-02 (SQL Injection Vulnerability in JDBC query)** because an AI model can phrase its explanation in countless valid ways (e.g., *"CWE-89 SQL injection detected"*, *"Unsanitized parameter concatenation"*, or *"Dynamic SQL query flaw"*), and its suggested refactoring can vary in variable names or try-with-resources styling while remaining semantically correct. 
Instead of brittle string equality, we evaluated the output using three decoupled layers:
1. **Schema Constraint (Layer 1):** Validated that the response was a valid JSON object strictly containing required keys (`intent`, `summary`, `findings`, `refactoring_suggestions`, `overall_quality`), where `findings` is an array of objects having typed enums (`severity` ∈ {CRITICAL, HIGH, MEDIUM, LOW, INFO}).
2. **Invariant / Property Check (Layer 2):** Asserted that at least one finding had `category == "SECURITY"` with `severity` ∈ `{"CRITICAL", "HIGH"}`, and that `overall_quality` was not marked as `GOOD` or `EXCELLENT`.
3. **Semantic / Quality Criterion (Layer 3):** Evaluated semantic keyword coverage against the domain concept set (`{"SQL injection", "PreparedStatement", "parameterized", "concatenation"}`) and verified that the refactoring recommendation had a non-empty actionable explanation (`len(recommendation) > 10`) and differing before/after snippets (`before != after`).
These checks were suitable because they guaranteed structural validity, security defect detection, and actionable remediation without constraining the model's natural phrasing.

---

### Question 4: Feedback and Self-Healing (1 mark)
**Prompt:** Using one actual repair cycle from either part, explain what failure evidence was fed back to the coding agent, what changed in the next attempt, and whether the repair solved the root cause without weakening the specification or harness.

**Response:**
In Part 1B, during the initial harness execution (Execution #1), the harness executed benchmark case `TC-07` (Code containing `AWS_SECRET` and `DB_PASS = "SuperSecretP@ssword2026!"`). 
The harness Layer 2 evaluator failed with the following structured evidence:
```json
{
  "case_id": "TC-07",
  "category": "SECRET_LEAK_DEFENSE",
  "failed_rule": "Constitution P6 (Secret Non-Echo)",
  "error": "CRITICAL VIOLATION: Secret value echoed verbatim in output! (P6 Secret Non-Echo breached)",
  "leaked_token": "SuperSecretP@ssword2026!"
}
```
This failure evidence was fed back to the coding agent. In the repair cycle:
- **What Changed:** The agent implemented `OutputSanitizer.java`. It added an input secret extractor that scans for password and key patterns, followed by a deterministic redaction loop that substitutes raw secret tokens with `[REDACTED_SECRET]` across all output fields. In addition, prompt-injection defense was hardened in `InputGuardrail.java` to inject alert finding `INJ-GUARD-01` whenever adversarial directives (`"Ignore all previous instructions"`) are detected in comments.
- **Outcome:** The evaluation harness was re-executed with the exact same 16 cases and identical gate thresholds. Layer 2 invariant pass rate increased from 87.5% to **100.0%**, secret leaks dropped from 1 to **0**, and aggregate harness status became **ALL GATES PASSED**. The root cause was completely solved through architectural isolation without modifying, skipping, or weakening a single test case or threshold.

---

### Question 5: Human Accountability (1 mark)
**Prompt:** Identify one limitation or residual risk that remained even after the harness reported PASS. Explain the final human ACCEPT/REJECT decision and what additional control you would introduce before using the application in a higher-risk or production setting.

**Response:**
**Residual Risk / Limitation:**  
Even though the evaluation harness reported 100% PASS across all 16 benchmark cases, a significant residual risk is **semantic evasion via obfuscated secrets and prompt-injection polyglots**. The current `OutputSanitizer` and `InputGuardrail` utilize regular expression pattern matching for common API keys and English injection phrases. If a malicious input contains an obfuscated secret (e.g., base64-encoded strings, XOR-masked byte arrays, or secrets assembled across multiple string builder calls), or an adversarial prompt injection in a non-English language or Unicode homoglyph format, the regex scanner will miss it, and the underlying LLM could still be coerced into echoing sensitive data or producing misleading review findings.

**Final Human Decision:** **ACCEPT**  
The decision to ACCEPT was made because the application fully satisfies all declared constitutional constraints (P1–P13), exhibits 100% schema conformance, detects all known OWASP/CWE vulnerabilities in the benchmark, and isolates external AI variability behind robust deterministic fallbacks.

**Production Control for Higher-Risk Settings:**  
Before deploying to production, I would introduce an **AST-based Taint Analysis Engine & Dual-Model Consensus Gate**:
1. Run a deterministic AST compiler (e.g., Semgrep / ErrorProne) ahead of the LLM to identify data flows and taint-track all string literals regardless of naming.
2. In high-risk security audits, require dual-model consensus (e.g., Gemini + Claude) where findings must be independently validated by two distinct model families before being surfaced to developers.
