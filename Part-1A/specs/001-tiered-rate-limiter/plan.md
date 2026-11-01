# Implementation Plan: 001-tiered-rate-limiter

## Technical Context
* **Runtime:** Java 17 (OpenJDK Temurin)
* **Framework:** Spring Boot 3.2.x (Web, Validation)
* **Build System:** Apache Maven with Maven Wrapper
* **Constitution Compliance:** Injected `java.time.Clock`, 0 Checkstyle violations, 0 SpotBugs findings, JaCoCo ≥ 90% line coverage, 100% AC traceability.

## Proposed Package Layout
```
edu.sezg534.ratelimiter
├── RateLimiterApplication.java
├── bucket
│   └── TokenBucket.java              # Pure deterministic math (Clock-driven)
├── store
│   ├── BucketStore.java              # Storage interface
│   └── InMemoryBucketStore.java      # Thread-safe in-memory adapter
├── config
│   ├── TierProperties.java           # @ConfigurationProperties + Bean Validation
│   └── ClockConfig.java              # Injected Clock bean
├── limiter
│   ├── Decision.java                 # Record
│   └── RateLimiterService.java       # Tier lookup & evaluation orchestration
└── web
    ├── RateLimitFilter.java          # OncePerRequestFilter enforcing limits & headers
    ├── ResourceController.java       # GET /api/resource
    ├── HealthController.java         # GET /health
    └── ApiErrorHandler.java          # Standard JSON 401 & 429 error representations
```

## Traceability & Verification Matrix
Every Acceptance Criterion (AC-01 through AC-14) is mapped to dedicated test cases in `src/test/java/edu/sezg534/ratelimiter` using `@AcceptanceCriterion("AC-xx")`.
All gates in `.harness/run_harness.py` will execute against this layout and generate structured run evidence.
