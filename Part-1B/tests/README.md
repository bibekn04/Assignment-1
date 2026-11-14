# Part 1B — Tests Directory Reference

This directory serves as the root pointer to the test suites for Part 1B.
Following standard Java/Maven conventions, the Java test sources are located in:

`src/test/java/edu/sezg534/codereview/`

### Test Suites Included:
1. **Contract Tests**:
   - `CodeReviewContractTest.java`: Spring MVC test suite verifying `/api/v1/review` and `/api/v1/health` endpoints, valid JSON response headers, schema invariants, and 400 Bad Request handling.
2. **Pre-Execution Guardrail Tests**:
   - `InputGuardrailTest.java`: Tests input length, printable ratio, empty code rejection, and comment prompt injection detection (`INJ-GUARD-01`).
3. **Post-Processing Sanitizer Tests**:
   - `OutputSanitizerTest.java`: Verifies zero-tolerance secret redaction (`[REDACTED_SECRET]`) across all response fields and refactoring diff integrity.

### Evaluation Benchmark Dataset & Harness:
In addition to the Java tests above, non-deterministic AI evaluation is performed via:
- Evaluation Dataset: `evals/dataset.json` (16 curated test cases across 8 categories).
- 3-Layer Evaluation Harness: `.harness/evaluator.py` & `.harness/run_harness.py`.

### Running Tests:
```bash
# Run Java unit & contract tests:
.\mvnw.cmd test

# Run 3-Layer AI Evaluation Harness against live service:
python .harness/run_harness.py
```

