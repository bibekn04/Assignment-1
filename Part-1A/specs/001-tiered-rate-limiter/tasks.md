# Tasks: 001-tiered-rate-limiter

## Phase 1: Setup & Build Tooling
* [ ] **T001**: Configure `pom.xml` with Spring Boot 3.2.x, Java 17 release, Checkstyle, SpotBugs + FindSecBugs, JaCoCo line coverage (≥90%), and Maven Wrapper.
* [ ] **T002**: Create `config/checkstyle.xml` and empty `config/spotbugs-exclude.xml`.

## Phase 2: Spec-Derived Tests (TDD / Test-First)
* [ ] **T003**: Create test support classes `MutableClock.java` and `@AcceptanceCriterion` annotation. [AC-04]
* [ ] **T004**: Implement unit tests for `TokenBucketTest` covering capacity bursts, refill rates, and clock advancement. [AC-01, AC-02, AC-03, AC-04, AC-05, AC-13]
* [ ] **T005**: Implement unit tests for `InMemoryBucketStoreTest` covering client isolation and concurrency. [AC-06, AC-13]
* [ ] **T006**: Implement unit tests for `TierPropertiesTest` validating configuration constraints and startup failures. [AC-07, AC-11]
* [ ] **T007**: Implement unit tests for `RateLimiterServiceTest` validating decision evaluations. [AC-01, AC-02, AC-03, AC-06, AC-07, AC-08, AC-10]
* [ ] **T008**: Implement MockMvc contract tests in `HttpContractTest` verifying HTTP status codes, standard headers, health bypass, unauthorized handling, and key masking. [AC-01, AC-02, AC-03, AC-08, AC-09, AC-10, AC-12, AC-14]

## Phase 3: Core Token Bucket & Storage Implementation
* [ ] **T009**: Implement `TokenBucket.java` with thread-safe token math and injected `Clock`. [AC-01, AC-02, AC-03, AC-04, AC-05, AC-13]
* [ ] **T010**: Implement `BucketStore.java` and `InMemoryBucketStore.java` using `ConcurrentHashMap`. [AC-06, AC-13]
* [ ] **T011**: Implement `Decision.java` record and `RateLimiterService.java` orchestrator. [AC-01, AC-02, AC-03, AC-06, AC-07, AC-08, AC-10]

## Phase 4: Web Layer & Error Handling
* [ ] **T012**: Implement `RateLimitFilter.java` intercepting requests, validating API key, enforcing limits, and injecting headers. [AC-01, AC-02, AC-03, AC-08, AC-09, AC-10, AC-12, AC-14]
* [ ] **T013**: Implement `ResourceController.java` (`GET /api/resource`) and `HealthController.java` (`GET /health`). [AC-01, AC-12]
* [ ] **T014**: Implement `ApiErrorHandler.java` providing structured JSON 401 & 429 bodies with API key masking. [AC-09, AC-10, AC-14]

## Phase 5: Configuration & Startup Validation
* [ ] **T015**: Implement `TierProperties.java` with `@ConfigurationProperties` and Bean Validation. [AC-07, AC-11]
* [ ] **T016**: Create `application.yml` and `tiers.example.yml` with synthetic test keys and tiers. [AC-07, AC-11, AC-14]

## Phase 6: Deterministic Harness & Documentation
* [ ] **T017**: Engineer independent Python evaluation harness `.harness/run_harness.py` with Gates 0–4. [AC-01..AC-14]
* [ ] **T018**: Execute harness, capture initial snapshots, run repair cycle if needed, and log results in `evidence/harness-log.md`.
* [ ] **T019**: Finalize `README.md` and complete performance benchmark (10,000 decisions < 1s).
