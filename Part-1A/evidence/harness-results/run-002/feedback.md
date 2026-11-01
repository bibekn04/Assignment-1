# HARNESS FAILURE FEEDBACK
**Failed Gate**: LINT

## Failure Summary
- **Message**: Checkstyle reported 1 violations.

### Violations / Findings
- src\main\java\edu\sezg534\ratelimiter\config\TierProperties.java:5 - Unused import - jakarta.validation.constraints.NotBlank.

### Process Output Snippet
```text
----
[INFO] Building tiered-rate-limiter 1.0.0
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- checkstyle:3.3.1:check (default-cli) @ tiered-rate-limiter ---
[INFO] Starting audit...
[WARN] C:\Java Codes\Assignment-1\Part-1A\src\main\java\edu\sezg534\ratelimiter\config\TierProperties.java:5:8: Unused import - jakarta.validation.constraints.NotBlank. [UnusedImports]
Audit done.
[WARNING] src\main\java\edu\sezg534\ratelimiter\config\TierProperties.java:[5,8] (imports) UnusedImports: Unused import - jakarta.validation.constraints.NotBlank.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.399 s
[INFO] Finished at: 2026-11-01T14:35:07+05:30
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin:3.3.1:check (default-cli) on project tiered-rate-limiter: You have 1 Checkstyle violation. -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

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