# Tiered Rate Limiter Microservice Constitution

## Core Principles

### I. Deterministic Time & Purity (NON-NEGOTIABLE)
All rate-limiting calculations, bucket refills, and expiration checks MUST depend strictly on an injected `java.time.Clock`. 
- Direct calls to `System.currentTimeMillis()`, `System.nanoTime()`, or no-argument `Instant.now()` in `src/main` are strictly forbidden.
- The core algorithm (`TokenBucket.java`) must be pure Java 17 with zero framework or I/O dependencies.

### II. Test-First & Independence (NON-NEGOTIABLE)
Every acceptance criterion (AC-01 through AC-14) must be formally specified with binary-evaluable assertions and mapped to independent automated tests tagged with `@AcceptanceCriterion("AC-xx")`.
- Acceptance tests must be written and locked before production code implementation.
- Coding agents are prohibited from modifying test assertions, deleting tests, or adding `@Disabled` annotations to achieve a passing status.

### III. Zero Secrets & Secure Defaults
No secrets, production credentials, or real client API keys may ever be stored in repository files.
- The demo configuration (`tiers.example.yml`) must contain only synthetic dummy keys.
- Client API keys must never be echoed in full in error bodies or log statements (AC-14).

### IV. Measurable Quality Gates
Production acceptance requires passing all 5 automated harness gates without manual intervention or threshold lowering:
1. **Gate 0 (Integrity):** Baseline test count, AC list, gate scripts, and configuration hashes match `.harness/baseline.json`.
2. **Gate 1 (Lint):** Checkstyle reports exactly 0 violations.
3. **Gate 2 (Security):** SpotBugs + FindSecBugs report 0 findings at the configured threshold with no unauthorized exclusions.
4. **Gate 3 (Unit & Contract Tests):** 100% test pass rate with JaCoCo line coverage of `src/main` ≥ 90%.
5. **Gate 4 (Traceability):** 100% of defined Acceptance Criteria mapped to at least 1 passing test.

### V. Performance Standard
Rate-limit evaluation must operate with O(1) time complexity. An in-process stress test of 10,000 consecutive decisions must complete within 1.0 second.

## Technology Stack & Environment Constraints

- **Language:** Java 17 (LTS)
- **Framework:** Spring Boot 3.2.x (Web, Validation)
- **Build Tool:** Maven with Maven Wrapper (`./mvnw`)
- **Evaluation Harness:** Python 3.10+ independent test runner invoking Maven goals and parsing XML/CSV artifacts.

## Governance & Upstream Traceability

The constitution supersedes all implementation shortcuts. If any specification defect or ambiguity arises during development, the resolution must be committed upstream to the Spec Kit artifacts (`spec.md`, `plan.md`, `tasks.md`) rather than patched silently in application code.

**Version:** 1.0.0 | **Ratified:** 2026-11-01 | **Status:** RATIFIED
