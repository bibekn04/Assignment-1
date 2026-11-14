# Part 1A — Tests Directory Reference

This directory serves as the root pointer to the deterministic test suites for Part 1A.
Following standard Java/Maven project structure, the test sources and acceptance criteria tests are located in:

`src/test/java/edu/sezg534/ratelimiter/`

### Test Suites Included:
1. **Unit Tests** (`src/test/java/edu/sezg534/ratelimiter/unit/`):
   - `TokenBucketTest.java`: Pure unit tests verifying token consumption, refill rates, clock drift, and multithreaded concurrency (`AC-01` through `AC-05`, `AC-13`).
   - `InMemoryBucketStoreTest.java`: Validates thread-safe client bucket isolation and concurrent instantiation (`AC-11`).
   - `RateLimiterServiceTest.java`: Tests tier mapping, unknown API keys, and decision telemetry metrics (`AC-06`, `AC-07`, `AC-08`).
   - `TierPropertiesTest.java`: Verifies configuration binding and Bean Validation constraints (`AC-09`, `AC-10`).

2. **HTTP Contract Tests** (`src/test/java/edu/sezg534/ratelimiter/contract/`):
   - `HttpContractTest.java`: Full Spring MVC `@SpringBootTest` testing HTTP headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`), `Retry-After` on 429 status, 401 Unauthorized for unknown/missing API keys, API key masking in error bodies, and `/actuator/health` exemption (`AC-01`, `AC-02`, `AC-03`, `AC-06`, `AC-07`, `AC-08`, `AC-12`, `AC-14`).

3. **Test Support** (`src/test/java/edu/sezg534/ratelimiter/support/`):
   - `MutableClock.java`: Deterministic, controllable time source.
   - `AcceptanceCriterion.java`: Annotation binding test methods to specification criteria for Gate 4 automated traceability verification.

### Running Tests:
```bash
# Direct Maven test run:
.\mvnw.cmd test

# Full 5-Gate Deterministic Harness:
python .harness/run_harness.py
```

