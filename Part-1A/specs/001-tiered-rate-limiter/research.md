# Technical Research & Architectural Decisions

## 1. Algorithm Selection: Token Bucket vs Alternatives

| Algorithm | Pros | Cons | Decision |
|---|---|---|---|
| **Token Bucket** | Accommodates controlled bursts up to capacity $C$; guarantees steady refill rate $R$; low memory footprint (two timestamps/counters per client). | Requires clock injection for deterministic simulation. | **SELECTED**. Directly matches requirement Sample B. |
| **Leaky Bucket** | Smoothes traffic to strict uniform rate. | Rejects bursts even when system is idle. | Rejected. |
| **Sliding Window Log** | High accuracy. | Memory consumption scales with request frequency (stores timestamps per request). | Rejected. |
| **Fixed Window Counter** | Very low memory. | Vulnerable to 2x boundary bursts at window resets. | Rejected. |

---

## 2. Refill Strategy: Lazy on-access vs Scheduled Background Threads

* **Lazy on-access:** Upon each incoming request, calculate $\Delta t = \text{now} - \text{lastRefillTime}$, compute newly generated tokens $\Delta t \times R$, add to existing balance up to capacity, and update $\text{lastRefillTime}$.
* **Scheduled Background Refill:** Periodically tick every bucket using a background thread pool.
* **Decision:** **Lazy on-access**. It requires zero background scheduler threads, eliminates thread contention on idle clients, avoids timer drift, and allows pure deterministic unit testing simply by advancing an injected `MutableClock`.

---

## 3. Concurrency Strategy

* Storage uses `ConcurrentHashMap<String, TokenBucket>` to store buckets per API key.
* Bucket mutation: Each `TokenBucket` instance encapsulates its own synchronization (thread-safe refill and consumption) so multiple concurrent requests for the same client are serialized without global application bottlenecks. Different clients proceed concurrently without blocking each other.

---

## 4. HTTP Layer: Filter vs HandlerInterceptor

* **Decision:** `OncePerRequestFilter`.
* **Rationale:** A Servlet Filter executes before Spring MVC handler mapping and controller dispatch. If a request is unauthenticated (401) or throttled (429), it rejects immediately with standard headers without invoking controller or reflection overhead.
