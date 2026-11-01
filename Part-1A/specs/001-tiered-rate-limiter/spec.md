# Feature Specification: 001-tiered-rate-limiter

## 1. Overview & Context

This feature implements an in-memory, tiered HTTP rate-limiting microservice based on the Token-Bucket algorithm for Part 1A of the AI-Augmented SDLC assignment. The service regulates inbound traffic by client tiers, responds with informative RFC-compliant HTTP headers, handles bursts up to configured capacity, smoothly refills tokens over time, and rejects excess requests with HTTP 429.

---

## 2. Clarifications (Settled Prior to Implementation)

The following ambiguities were evaluated and formally resolved:

1. **Authentication Policy for Missing/Unknown Keys:**
   * *Resolution:* Missing or unrecognized API keys return HTTP `401 Unauthorized` with a structured JSON error body. They do not consume tokens.
2. **`X-RateLimit-Reset` Format:**
   * *Resolution:* The `X-RateLimit-Reset` header is represented as a Unix epoch timestamp in seconds (e.g. `1761993600`) indicating when the bucket will be completely full.
3. **Token Consumption on Rejection:**
   * *Resolution:* Rejected requests (HTTP 429) do **not** decrement available tokens.
4. **Token Granularity:**
   * *Resolution:* Tokens are computed as floating-point quantities (`double`) internally to prevent rounding drift; `X-RateLimit-Remaining` is exposed as an integer using `Math.floor(tokens)`.
5. **Client Identification:**
   * *Resolution:* Requests are authenticated exclusively via the `X-API-Key` request header.
6. **Endpoints:**
   * *Resolution:* The service provides a protected demo endpoint `GET /api/resource` and an unconstrained endpoint `GET /health` (which is never rate-limited and requires no API key).

---

## 3. User Stories

### Story 1: Tier-Based Traffic Regulation (Priority: P1)
As an API service provider, I want incoming API requests to be capped based on the client's subscribed tier (Free, Pro, Enterprise) so that no client consumes more resources than allocated.

### Story 2: Rate Limit Telemetry & 429 Feedback (Priority: P1)
As an API client developer, I want all responses to include clear rate-limit telemetry headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`) and a `Retry-After` header when throttled (HTTP 429), so that my client can automatically pace retries without polling blindly.

### Story 3: Robust Configuration & Health Monitoring (Priority: P2)
As a systems operator, I want invalid tier configurations to fail fast during application bootstrap and a `/health` endpoint to monitor service vitality without rate-limit interference.

---

## 4. Acceptance Criteria (AC-01 through AC-14)

All acceptance criteria are binary-evaluable and mapped to automated tests:

* **AC-01:** A new client on a tier with capacity $C$ can make $C$ consecutive requests at the same instant ($t=0$), and all $C$ requests return HTTP `200 OK`.
* **AC-02:** Immediate request $C+1$ at that same instant ($t=0$) returns HTTP `429 Too Many Requests`.
* **AC-03:** A `429` response includes a `Retry-After` header equal to $\lceil \text{seconds until } 1 \text{ token is available} \rceil$, with a minimum value of `1`.
* **AC-04:** When time advances by $1 / \text{refill\_rate}$ seconds, exactly one additional request succeeds with HTTP `200 OK`.
* **AC-05:** After any extended idle period, the available tokens for a client never exceed the configured tier `capacity`.
* **AC-06:** Token buckets are strictly isolated per API key: exhausting client A's token quota has zero effect on client B.
* **AC-07:** Each configured tier (`free`, `pro`, `enterprise`) enforces its own distinct configured `capacity` and `refill_rate`.
* **AC-08:** Every request processed through the limiter includes `X-RateLimit-Limit`, `X-RateLimit-Remaining` (non-negative integer), and `X-RateLimit-Reset` (epoch seconds).
* **AC-09:** A request missing the `X-API-Key` header returns HTTP `401 Unauthorized` with a JSON body and consumes zero tokens.
* **AC-10:** A request presenting an unrecognized `X-API-Key` returns HTTP `401 Unauthorized` and consumes zero tokens.
* **AC-11:** Invalid tier configuration (capacity $\le 0$, refill_rate $\le 0$, duplicate tier names, or an API key mapped to an undefined tier) causes startup failure with an informative error message.
* **AC-12:** `GET /health` returns HTTP `200 OK` regardless of rate limits and requires no API key.
* **AC-13:** Under concurrent load ($N > C$ simultaneous requests against capacity $C$), exactly $C$ requests succeed and $N - C$ requests return 429 (thread safety).
* **AC-14:** API error bodies and application logs NEVER disclose or echo full API keys (keys are masked e.g. `key_***1234`).

---

## 5. Out of Scope

The following items are explicitly out of scope for Part 1A:
* Distributed storage backends (e.g., Redis).
* Authentication mechanisms beyond static API key lookup.
* Dynamic endpoint route weighting or per-route limits.
* Web user interfaces or administrative dashboards.
