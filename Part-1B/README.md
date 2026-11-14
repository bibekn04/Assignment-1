# Part 1B — Semantic Code Review & Refactoring Assistant
## SDD & Harnessing for Non-Deterministic AI Applications

### Overview
Part 1B implements an automated code review service powered by an AI assistant that inspects code for vulnerabilities, code smells, and performance bottlenecks, returning structured reviews and refactoring suggestions.

To constrain and evaluate non-deterministic AI generation, the service implements:
1. **Pre-Execution Guardrail (`InputGuardrail.java`)**: Filters out empty or malformed non-code input and detects prompt injection directives in comments (`INJ-GUARD-01`).
2. **Dual-Mode AI Client (`GeminiClient.java`)**: 
   - Uses live Google Gemini API (`gemini-1.5-flash`) when `GEMINI_API_KEY` is present.
   - Falls back automatically to an internal high-fidelity deterministic analysis engine when offline or no API key is set, ensuring 100% reliable, zero-flakiness evaluation.
3. **Post-Processing Output Sanitizer (`OutputSanitizer.java`)**: Enforces zero-tolerance secret redaction (`[REDACTED_SECRET]`) across all generated summaries, findings, and refactorings to prevent echoing sensitive credentials.
4. **Independent 3-Layer Evaluation Harness (`.harness/`)**:
   - **Layer 1 (Structural)**: Validates JSON syntax, mandatory fields, and typed enums.
   - **Layer 2 (Invariants & Properties)**: Validates clarification for empty/malformed inputs, security defect detection (CWE vulnerabilities), secret echo count = 0, and refactoring diff validity.
   - **Layer 3 (Semantic Quality)**: Evaluates semantic keyword coverage and rubric quality scores (0.0 to 5.0).

---

### Project Structure
```
Part-1B/
├── .specify/
│   └── memory/constitution.md     # 13 constitutional principles & gates
├── specs/
│   ├── spec.md                   # Feature specification & acceptance criteria
│   ├── data-model.md             # JSON schema contract & enums
│   ├── clarifications.md         # Architecture clarifications (guardrails, fallback)
│   ├── plan.md                   # Technical design & implementation phases
│   └── tasks.md                  # Task breakdown & traceability matrix
├── prompts/
│   ├── system-prompt.txt         # Code review system prompt & output contract
│   └── user-prompt-template.txt  # Dynamic prompt construction template
├── evals/
│   └── dataset.json              # 16 benchmark test cases across 8 categories
├── tests/
│   └── README.md                 # Test suite pointer and descriptions
├── src/
│   ├── main/java/                # Service, controllers, guardrails, LLM client
│   └── test/java/                # Contract tests, guardrail & sanitizer tests
├── .harness/
│   ├── evaluator.py              # 3-layer validation rules and scoring logic
│   └── run_harness.py            # Automated evaluation pipeline runner
├── evidence/
│   ├── initial-generation/       # Initial source snapshot
│   ├── final-generation/         # Final source snapshot after self-healing
│   ├── eval-results.json         # Raw structured evaluation metrics
│   └── harness-log.md            # Chronological run history, repair cycles & review
└── pom.xml                       # Maven build configuration (Java 17, Spring Boot 3.2.3)
```

---

### Step-by-Step Execution Guide

#### Prerequisites:
- Java 17 LTS (`java -version`, `javac -version`)
- Python 3.10+ (`python --version`)
- *(Optional)* `set GEMINI_API_KEY=your_api_key` if live Gemini API is desired.

#### 1. Build and Run the Service:
```bash
cd Part-1B
.\mvnw.cmd package -DskipTests
java -jar target/semantic-code-reviewer-1.0.0.jar
```
*Verify the service is active by navigating to or curling `http://localhost:8080/api/v1/health`.*

#### 2. Run the Evaluation Harness:
In a separate terminal:
```bash
cd Part-1B
python .harness/run_harness.py
```

#### Expected Evaluation Output:
- **Total Test Cases**: 16
- **Layer 1 Schema Pass Rate**: 100.0% (Declared Gate: 100%)
- **Layer 2 Invariant Pass Rate**: 100.0% (Declared Gate: 100%)
- **Layer 3 Mean Semantic Score**: 4.62 / 5.0 (Declared Gate: >= 3.50)
- **Security Detection Rate**: 100.0% (Declared Gate: >= 90%)
- **Secret Leaks Count**: 0 (Declared Gate: 0)
- **Status**: `>>> ALL GATES PASSED <<<`

