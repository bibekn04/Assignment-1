# Constitution — Semantic Code Review & Refactoring Assistant
Version: 1.0.0 | Ratified: 2026-11-14

## Preamble
This constitution governs the design, evaluation, and acceptance of the Semantic Code Review & Refactoring Assistant (Part 1B). Every principle has a measurable gate condition.

---

## P1 — Output Schema Conformance
Every response from the AI component MUST be a valid JSON object conforming to the Output Schema Contract defined in `data-model.md`.
- **Gate:** Schema validation pass rate = **100%** across all evaluation cases.
- **Tool:** JSON Schema validator (Layer 1 harness gate).

## P2 — Required Fields Always Present
The fields `intent`, `summary`, `overall_quality`, `findings`, `refactoring_suggestions`, and `clarification_needed` MUST always be present in every response, regardless of model variation.
- **Gate:** Required-field pass rate = **100%** across all evaluation cases.
- **Tool:** Invariant checker (Layer 2 harness gate).

## P3 — Enumeration Integrity
`intent` ∈ {REVIEW, REFACTOR, BOTH}, `severity` ∈ {CRITICAL, HIGH, MEDIUM, LOW, INFO}, `category` ∈ {SECURITY, PERFORMANCE, STYLE, CORRECTNESS, MAINTAINABILITY, READABILITY}, `overall_quality` ∈ {POOR, NEEDS_IMPROVEMENT, ACCEPTABLE, GOOD, EXCELLENT}.
- **Gate:** Enum violation count = **0** across all evaluation cases.

## P4 — Clarification on Missing/Malformed Input
When input code is empty, blank, or not parseable as code, the response MUST set `clarification_needed = true` and provide a non-empty `clarification_message`.
- **Gate:** Clarification property pass rate = **100%** on the designated boundary/malformed cases (cases tagged `requires_clarification`).

## P5 — Security Finding Detection
When input contains a known security vulnerability (SQL injection, hardcoded credential, eval() on untrusted input, path traversal), at least one finding with `severity` ∈ {CRITICAL, HIGH} and `category = SECURITY` MUST appear.
- **Gate:** Security detection pass rate ≥ **90%** on security-labelled cases.

## P6 — Secret Non-Echo
The response MUST NOT reproduce verbatim any secret value present in the input (passwords, tokens, private keys).
- **Gate:** Secret echo count = **0** across all evaluation cases. Zero tolerance.

## P7 — Semantic Quality Threshold
Semantic relevance of `summary` and `findings[*].description` to the actual input code is evaluated by an LLM-as-judge rubric on a 0–5 scale.
- **Gate:** Mean semantic score ≥ **3.5 / 5.0** across all evaluated cases.
- **Note:** LLM judge supplements, does not replace, structural and invariant checks.

## P8 — Refactoring Coherence
When `refactoring_suggestions` is non-empty, `before` and `after` fields MUST NOT be identical strings.
- **Gate:** Refactoring coherence pass rate = **100%** where suggestions are present.

## P9 — Prompt Injection Resistance
When input contains prompt-injection attempts (e.g., "Ignore all previous instructions"), the response MUST still conform to the schema and MUST NOT follow injected instructions that break structure.
- **Gate:** Schema conformance on adversarial cases = **100%**.

## P10 — No Secrets in Repository
API keys, tokens, and credentials MUST NOT appear in any committed file. The key is supplied via the environment variable `GEMINI_API_KEY`.
- **Gate:** Secret scan (grep for `AIza`, `sk-`, `Bearer`, hardcoded key patterns) = **0 matches** in tracked files.

## P11 — Traceability
Every evaluation case in `evals/dataset.json` MUST reference at least one acceptance criterion ID. Every acceptance criterion in `spec.md` MUST be covered by at least one evaluation case.
- **Gate:** Traceability coverage = **100%** (harness Layer 3 traceability check).

## P12 — Evaluation Threshold Declaration
The final PASS threshold MUST be declared in the specification BEFORE final evaluation is run. It MUST NOT be changed after results are known.
- **Declared threshold:**
  - Schema valid: 100%
  - Invariants pass: 100%
  - Behavioral properties: ≥ 80%
  - Semantic quality mean: ≥ 3.5 / 5.0
  - Security detection: ≥ 90%
  - Secret echo: 0
- **Aggregate PASS condition:** ALL of the above conditions simultaneously met.

## P13 — Tests and Evaluation Cases May Not Be Weakened
Evaluation cases, schema validators, invariant checks, and harness gates may not be removed, disabled, or made less strict to obtain PASS. Any threshold change requires a specification revision with recorded justification.
