# Part 1A: Tiered Rate Limiter Microservice (Deterministic SDD & Harnessing)

This project implements a deterministic, tiered HTTP rate-limiting microservice in Java 17 using the Token-Bucket algorithm and Spec-Driven Development (SDD) per GitHub Spec Kit standards.

---

## Prerequisites

* **Java Development Kit:** Java 17 (LTS) — OpenJDK / Temurin (`java -version`, `javac -version`)
* **Python Runtime:** Python 3.10+ with `uv` (`python --version`)
* **Build System:** Maven (Maven Wrapper `./mvnw` provided in project root)

---

## Quick Start

### 1. Build and Run the Microservice
```bash
# Using Maven Wrapper (Windows CMD / PowerShell)
.\mvnw.cmd spring-boot:run

# Or standard Bash
./mvnw spring-boot:run
```
The service will start on port `8080`.

### 2. Run the Deterministic Evaluation Harness
The independent Python harness executes all 5 evaluation gates (Integrity, Lint, SAST Security, Tests/Coverage, Traceability) in sequence:
```bash
# From Part-1A directory:
uv run python .harness/run_harness.py
```

---

## Architecture & Verification Gates

| Gate # | Name | Tool | Requirement |
|---|---|---|---|
| **Gate 0** | **Integrity Guard** | Custom | Verifies no tests deleted or disabled (`@Disabled`), configs unchanged vs `baseline.json` |
| **Gate 1** | **Lint Guard** | Checkstyle 10.14 | Exactly **0** style or non-deterministic time violations |
| **Gate 2** | **SAST Guard** | SpotBugs + FindSecBugs | Exactly **0** security findings at configured threshold |
| **Gate 3** | **Tests & Coverage** | JUnit 5 + JaCoCo | 100% test pass rate + JaCoCo line coverage **≥ 90%** |
| **Gate 4** | **Traceability Guard** | Custom | 100% of Acceptance Criteria (AC-01..AC-14) mapped to passing tests |

---

## Directory Structure

```
Part-1A/
├── .specify/            # GitHub Spec Kit templates, constitution, scripts
│   └── memory/
│       └── constitution.md
├── specs/
│   └── 001-tiered-rate-limiter/
│       ├── spec.md      # Acceptance criteria AC-01 through AC-14
│       ├── plan.md      # Architecture and tech plan
│       ├── research.md  # Architectural trade-off analysis
│       ├── data-model.md
│       ├── contracts/openapi.yaml
│       ├── tasks.md     # Traceable task breakdown
│       └── analysis.md  # Cross-artifact consistency report
├── config/
│   ├── checkstyle.xml
│   └── spotbugs-exclude.xml
├── .harness/            # Deterministic evaluation harness
│   ├── run_harness.py   # Master harness runner
│   ├── baseline.json    # Locked test and criteria baseline
│   ├── feedback.py      # Self-healing feedback generator
│   └── gates/           # Gate scripts (0 through 4)
├── src/
│   ├── main/            # Application production code
│   └── test/            # Spec-derived unit and contract tests
├── evidence/
│   ├── initial-generation/ # Snapshot of pre-repair code
│   ├── final-generation/   # Snapshot of passing code
│   ├── harness-results/    # Numbered execution runs (run-001, run-002, run-003)
│   └── harness-log.md      # Chronological audit trail & human review
└── pom.xml              # Maven dependencies & build plugins
```

---

## API Endpoints

* `GET /health`: Health check endpoint. Unconstrained (never rate limited, requires no API key).
* `GET /api/resource`: Protected endpoint. Requires `X-API-Key` header.
  * Headers returned:
    * `X-RateLimit-Limit`: Maximum tier capacity.
    * `X-RateLimit-Remaining`: Available tokens remaining.
    * `X-RateLimit-Reset`: Unix timestamp when bucket will be fully refilled.
  * Throttled response (HTTP 429):
    * `Retry-After`: Seconds to wait before 1 token becomes available.
