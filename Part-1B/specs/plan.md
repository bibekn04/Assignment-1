# Technical Plan & Architecture
**Part 1B: Semantic Code Review & Refactoring Assistant**

---

## 1. System Architecture Pipeline

The system is engineered as an AI-augmented microservice with deterministic safety outer layers protecting against non-deterministic AI variance:

```
[ HTTP POST /api/v1/review ]
           │
           ▼
[ Layer A: Input Guardrail ]
   ├── Blank / Empty Check ───► (Returns Clarification Needed immediately)
   ├── Binary / Corrupt Data Check
   └── Prompt Injection Detection
           │
           ▼
[ Layer B: Prompt Assembly Engine ]
   ├── Strict System Role Instructions (JSON Contract, Enum Bounds)
   └── User Code Escaping & Context Injection
           │
           ▼
[ Layer C: LLM Client Execution ]
   ├── Primary: Gemini 1.5 Flash / Pro API Client (if GEMINI_API_KEY present)
   └── Secondary / Fallback: Deterministic High-Fidelity Local Engine (rule-based analysis)
           │
           ▼
[ Layer D: Output Parser & Schema Enforcer ]
   ├── Markdown Code Fence Stripping
   ├── Strict JSON Parsing & Invariant Verification
   └── Fallback Schema Repair (if model produces partial or unclosed JSON)
           │
           ▼
[ Layer E: Output Sanitizer & Secret Redactor ]
   ├── Secret Scanning (Redacts credentials, tokens, keys)
   └── Enum & Coherence Sanitizer (Validates Before != After in refactoring)
           │
           ▼
[ Clean HTTP 200 JSON Response ]
```

---

## 2. Technology Stack
- **Language:** Java 17 (matching Part 1A)
- **Framework:** Spring Boot 3.2.4 (embedded Tomcat REST service)
- **Serialization / Validation:** Jackson 2.15+, Jakarta Validation
- **Evaluation Harness:** Python 3 pipeline (`.harness/evaluator.py`, `.harness/run_harness.py`)
- **Dataset:** `evals/dataset.json` containing 16 diverse benchmark cases.

---

## 3. Evaluation Harness Design

The harness operates as a 3-tier validation pipeline:
1. **Layer 1: Structural Gate**
   - Validates JSON parsing and JSON Schema compliance.
   - Enforces required top-level fields and non-null properties.
   - Enforces enumeration constraints on `intent`, `severity`, `category`, and `overall_quality`.
2. **Layer 2: Property & Invariant Gate**
   - Invariant: `clarification_needed == true` when input is malformed or empty.
   - Invariant: `category == SECURITY` with `severity in [CRITICAL, HIGH]` on known vulnerability cases.
   - Invariant: `before_snippet != after_snippet` for all refactorings.
   - Invariant: Zero secret leakage (`[REDACTED_SECRET]` masking check).
3. **Layer 3: Semantic Quality Gate**
   - Verifies presence of meaningful `summary` and `findings`.
   - Keyword and semantic coverage against ground-truth rubrics.
   - Computes normalized score (0.0 to 5.0) per case and aggregate mean.
