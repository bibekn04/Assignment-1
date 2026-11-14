# AI-Augmented SDLC (SEZG534) - Assignment 1
## Spec-Driven Development & Harness Engineering

### Repository Overview
This repository contains the complete Spec-Driven Development (SDD) artifacts, implementations, independent evaluation harnesses, and verification evidence for Assignment 1:
- **Part 1A**: Deterministic SDD & Harnessing — Tiered Rate Limiter Microservice (Java 17, Spring Boot, Token-Bucket algorithm, 5-gate deterministic harness).
- **Part 1B**: Non-Deterministic AI SDD & Harnessing — Semantic Code Review & Refactoring Assistant (Java 17, Spring Boot, dual-mode Gemini/local engine, 3-layer probabilistic evaluation harness across 16 benchmark cases).
- **Part 2**: Reflection Questions & LMS Submission Report (`Part-2-Reflection-Answers.md`).

---

### Repository Structure
```
Assignment-1/
├── README.md
├── Part-2-Reflection-Answers.md
├── Part-1A/
│   ├── .specify/            # GitHub Spec Kit memory & constitution
│   ├── specs/               # Feature specification, technical plan, data model, tasks
│   ├── tests/               # Pointer to unit & contract test suites
│   ├── pom.xml              # Maven build file (Java 17 baseline)
│   ├── src/                 # Application source and tests
│   ├── .harness/            # Deterministic harness gates (Integrity, Lint, SAST, Tests, Traceability)
│   └── evidence/            # Snapshots, harness run logs, and human review decisions
└── Part-1B/
    ├── .specify/            # Part 1B constitution with measurable probabilistic constraints
    ├── specs/               # Feature spec, technical plan, schema contract, tasks
    ├── prompts/             # System prompt & user prompt templates
    ├── evals/               # dataset.json (16 benchmark cases across 8 failure categories)
    ├── tests/               # Pointer to unit, contract, and guardrail test suites
    ├── pom.xml              # Maven build file (Java 17, Spring Boot 3.2.3)
    ├── src/                 # Source code, guardrails, LLM client, and contract tests
    ├── .harness/            # Non-deterministic evaluation harness (evaluator.py, run_harness.py)
    └── evidence/            # Initial/final snapshots, eval-results.json, harness-log.md
```

---

### Prerequisites
- **Java 17 (LTS)** (`java -version`, `javac -version`)
- **Python 3.10+** (`python --version`)
- Optional for Part 1B Live Gemini AI: `GEMINI_API_KEY` environment variable. *(Note: If absent, Part 1B automatically engages its high-fidelity deterministic engine, guaranteeing 100% reproducible offline evaluation).*

---

### Running Part 1A (Deterministic Harness)
```bash
cd Part-1A
python .harness/run_harness.py
```
**Expected Outcome:**
- Gate 0 (Integrity): PASS
- Gate 1 (Lint / Checkstyle): PASS
- Gate 2 (SAST / SpotBugs & FindSecBugs): PASS
- Gate 3 (Deterministic Concurrency & Contract Tests): PASS (23/23 tests pass, line coverage >= 90%)
- Gate 4 (Traceability): PASS (100% AC coverage)
- Final Result: `>>> ALL DETERMINISTIC GATES PASSED <<<`

---

### Running Part 1B (Non-Deterministic AI Harness)

#### Step 1: Start the Code Review Assistant Service
```bash
cd Part-1B
.\mvnw.cmd package -DskipTests
java -jar target/semantic-code-reviewer-1.0.0.jar
```
*(Verify service is UP: `curl http://localhost:8080/api/v1/health`)*

#### Step 2: Run the Single Evaluation Harness
In another terminal:
```bash
cd Part-1B
python .harness/run_harness.py
```

**Expected Outcome:**
- 16 benchmark cases evaluated across Normal, Boundary, Malformed, Security, and Adversarial categories
- Layer 1 Schema Pass Rate: **100.0%** (Gate: 100%)
- Layer 2 Invariant Pass Rate: **100.0%** (Gate: 100%)
- Layer 3 Mean Semantic Score: **4.62 / 5.0** (Gate: ≥ 3.50)
- Security Detection Rate: **100.0%** (Gate: ≥ 90%)
- Secret Leaks Count: **0** (Gate: 0)
- Final Result: `>>> ALL GATES PASSED <<<`
- Evidence output saved to: `Part-1B/evidence/eval-results.json` and `Part-1B/evidence/harness-log.md`

---

### Part 2: Reflection Questions
See [Part-2-Reflection-Answers.md](file:///C:/Java%20Codes/Assignment-1/Part-2-Reflection-Answers.md) for detailed responses to the 5 assessed reflection questions.
