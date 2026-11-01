# Data Model & Schema Definitions

## 1. Domain Entities

### `TierConfig`
Defines operational parameters for an authorization tier.
* `name`: `String` (e.g., `"free"`, `"pro"`, `"enterprise"`)
* `capacity`: `int` (Maximum burst capacity, must be $\ge 1$)
* `refillRate`: `double` (Tokens generated per second, must be $> 0$)

### `Decision` (Java Record)
Represents the outcome of a rate-limit evaluation.
* `allowed`: `boolean` (True if request is permitted, false if throttled)
* `limit`: `long` (Client bucket capacity)
* `remaining`: `long` (Available integer tokens remaining)
* `resetEpochSeconds`: `long` (Unix timestamp in seconds when bucket fully refills)
* `retryAfterSeconds`: `long` (Seconds client must wait before 1 token becomes available, $\ge 1$ on rejection)

### `TokenBucket`
Encapsulates thread-safe token mathematics.
* `capacity`: `double`
* `refillRate`: `double`
* `tokens`: `double`
* `lastRefillNanos`: `long` or `Instant`
* Injected: `java.time.Clock`

### `BucketStore` (Interface)
Storage contract separating rate limit state from persistence layer.
* `TokenBucket getOrCreate(String clientId, TierConfig tier)`

---

## 2. Configuration Schema (`application.yml`)

```yaml
ratelimiter:
  tiers:
    free:
      capacity: 5
      refill-rate: 1.0 # 1 token per second
    pro:
      capacity: 20
      refill-rate: 5.0 # 5 tokens per second
    enterprise:
      capacity: 100
      refill-rate: 25.0
  client-tiers:
    "key_free_123": "free"
    "key_pro_456": "pro"
    "key_ent_789": "enterprise"
```
