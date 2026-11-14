# Task Breakdown & Implementation Traceability
**Part 1B: Semantic Code Review & Refactoring Assistant**

---

## Task Matrix

| Task ID | Component | Description | Traceable ACs | Status |
|---|---|---|---|---|
| **TASK-01** | Spec Kit Artifacts | Formulate Constitution, Spec, Plan, Data Model, Tasks | P1–P13, AC-S01–S04 | COMPLETE |
| **TASK-02** | Evaluation Dataset | Author 16 curated test cases in `evals/dataset.json` covering normal, boundary, malformed, security, and prompt injection | AC-P01, AC-P02, AC-P03, AC-P06 | COMPLETE |
| **TASK-03** | Core Data Model | Implement Java DTOs (`ReviewRequest`, `ReviewResponse`, `Finding`, Enums) | AC-S01, AC-S02, AC-S03, AC-S04 | COMPLETE |
| **TASK-04** | Input Guardrail | Implement pre-execution guardrail detecting empty inputs, non-code garbage, and prompt injection attempts | AC-P01, AC-P06 | COMPLETE |
| **TASK-05** | Output Sanitizer | Implement regex-based credential redactor to prevent echoing secrets from inspected code | AC-P03 | COMPLETE |
| **TASK-06** | LLM Engine Layer | Implement dual-mode LLM Client (Gemini API with API key + local high-fidelity fallback engine) | AC-S01, AC-P02, AC-P05 | COMPLETE |
| **TASK-07** | Service Orchestrator | Connect guardrails, prompting, LLM call, schema repair, and sanitization into `CodeReviewService` | AC-S01–AC-Q03 | COMPLETE |
| **TASK-08** | REST Controller | Expose `/api/v1/review` and `/api/v1/health` with Spring Boot | REQ-1B-01 | COMPLETE |
| **TASK-09** | Unit & Contract Tests | Develop deterministic test suite verifying guardrails, sanitizers, and schema contract | AC-S01–AC-P06 | COMPLETE |
| **TASK-10** | Python Harness | Build 3-layer evaluation harness (`evaluator.py`, `run_harness.py`) | AC-S01–AC-Q03 | COMPLETE |
| **TASK-11** | Evaluation & Repair | Execute initial harness run, capture failure evidence, implement repair, and re-evaluate | Principle P12 | COMPLETE |
| **TASK-12** | Evidence & Submission | Archive logs in `evidence/`, draft reflection report answers, prepare Git repo | Checklist | COMPLETE |
