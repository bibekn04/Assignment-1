# AI-Augmented SDLC (SEZG534) - Assignment 1
## Spec-Driven Development & Harness Engineering

### Repository Overview
This repository contains the complete Spec-Driven Development (SDD) artifacts, implementations, independent evaluation harnesses, and verification evidence for Assignment 1.

* **Part-1A**: Deterministic SDD & Harnessing — Tiered Rate Limiter Microservice (Java 17, Spring Boot, Token-Bucket algorithm).
* **Part-1B**: Non-Deterministic AI SDD & Harnessing (to be added).

### Structure
```
Assignment-1/
├── README.md
├── .gitignore
├── Part-1A/
│   ├── .specify/            # GitHub Spec Kit artifacts & constitution
│   ├── specs/               # Feature specification, technical plan, data model, tasks
│   ├── pom.xml              # Maven build file (Java 17 baseline)
│   ├── src/                 # Application source and tests
│   ├── .harness/            # Deterministic harness gates (Integrity, Lint, SAST, Tests, Traceability)
│   └── evidence/            # Snapshots, harness run logs, and human review decisions
└── Part-1B/
```

### Quick Start (Part 1A)
Requirements:
* **Java 17 (LTS)** (`java -version`, `javac -version`)
* **Python 3.10+** with `uv` (`python --version`)

To execute the Part 1A Deterministic Harness:
```bash
cd Part-1A
python .harness/run_harness.py
```
