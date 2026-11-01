# HARNESS FAILURE FEEDBACK
**Failed Gate**: TESTS

## Failure Summary
- **Message**: Tests: 0/0 passed. Line Coverage: 0.0% (Required: >=90%)

### Process Output Snippet
```text
B
Progress (1): 1.9/2.3 MB
Progress (1): 1.9/2.3 MB
Progress (1): 1.9/2.3 MB
Progress (1): 1.9/2.3 MB
Progress (1): 1.9/2.3 MB
Progress (1): 1.9/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.0/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.1/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.2/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3/2.3 MB
Progress (1): 2.3 MB    
                    
Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/maven/surefire/surefire-shared-utils/3.1.2/surefire-shared-utils-3.1.2.jar (2.3 MB at 3.3 MB/s)
[INFO] No tests to run.
[INFO] 
[INFO] --- jacoco:0.8.11:report (default-cli) @ tiered-rate-limiter ---
[INFO] Skipping JaCoCo execution due to missing execution data file.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  9.132 s
[INFO] Finished at: 2026-11-01T14:21:47+05:30
[INFO] ------------------------------------------------------------------------

```

---
## Agent Instructions for Self-Healing
1. Fix the production code in `src/main/` so these gates pass.
2. Do NOT modify tests, specs, harness scripts, or gate configuration files.
3. Protected files that MUST NOT be edited:
   - `src/test/**`
   - `.harness/**`
   - `specs/**`
   - `config/checkstyle.xml`
   - `config/spotbugs-exclude.xml`
   - `pom.xml (coverage/lint/sast plugin thresholds)`

If this failure reveals a genuine defect in the specification, report it instead of hacking the test.