# Rate Limiting Research: bucket4j for Usage-Based Billing

**Last Updated:** 2025-12-15

This document contains research findings and architectural recommendations for implementing rate limiting in the
customer-api service using bucket4j.

---

## Business Requirements

1. **Daily resetting limits** - Limits should reset at a specific time (e.g., midnight UTC)
2. **On-the-fly changeable limits** - Limits can be adjusted without restarting the service
3. **Leniency allowance** - Customers can exceed their limit twice per month (grace period)
4. **Reusable across endpoints** - Single implementation usable across all customer-facing endpoints
5. **Varying limits per customer** - Different customers/tiers have different limits
6. **Usage-based billing** - More requests = higher billing

---

## Current Implementation

Location: `src/main/java/io/hephaistos/flagforge/customerapi/controller/security/RateLimitFilter.java`

Current setup uses:

- `bucket4j-core` (in-memory only)
- Per-minute limits with `Refill.greedy()`
- `ConcurrentHashMap<UUID, Bucket>` storage (lost on restart)
- Static limits read from `ApiKeySecurityContext.getRateLimitPerMinute()`

---

## Research Findings

### Requirement 1: Daily Resetting Limits

**Verdict: Fully supported by bucket4j**

bucket4j provides `Refill.intervallyAligned()` which allows configuration of limits that reset at specific times (e.g.,
midnight UTC):

```java
Instant nextMidnight = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant();

Bandwidth dailyLimit =
        Bandwidth.builder().capacity(10000).refillIntervallyAligned(10000, Duration.ofDays(1), nextMidnight).build();
```

This ensures all tokens are restored exactly at midnight, not on a rolling 24-hour window.

---

### Requirement 2: On-the-Fly Changeable Limits

**Verdict: Fully supported by bucket4j**

bucket4j supports runtime configuration replacement via `bucket.replaceConfiguration(newConfig, strategy)`.

**TokensInheritanceStrategy options:**

| Strategy         | Behavior                                    | Use Case                          |
|------------------|---------------------------------------------|-----------------------------------|
| `RESET`          | Clear all tokens, start fresh               | Customer upgrades/downgrades plan |
| `PROPORTIONALLY` | Scale tokens: `new = old * (newCap/oldCap)` | Fair mid-cycle upgrades           |
| `AS_IS`          | Keep tokens, cap at new max                 | Conservative approach             |
| `ADDITIVE`       | Keep + add capacity difference              | Generous upgrades                 |

**Important caveat:** You cannot change the *number* of bandwidth limits (e.g., from 1 to 2 limits) without an
`IncompatibleConfigurationException`. Design your configuration structure upfront.

---

### Requirement 3: Leniency (2x Monthly Breach Allowance)

**Verdict: NOT natively supported by bucket4j - requires custom implementation**

bucket4j does not have a concept of "allow N limit breaches per month." The token bucket algorithm either allows or
blocks - there's no built-in grace period or breach counter.

**Options:**

#### Option A: Overdraft Approach (Partial Solution)

Use bucket4j's capacity as "burst allowance." A bucket with capacity 10,000 and daily refill of 10,000 inherently allows
some overdraft within a day. However, this doesn't give you the "twice per month" semantic.

#### Option B: Leniency Service Layer (Recommended)

Implement a separate leniency tracking mechanism on top of bucket4j:

```
+-----------------------------------------------------------+
|                    RateLimitService                       |
+-----------------------------------------------------------+
| 1. Check bucket4j -> tokens available?                    |
|    |-- YES -> allow request                               |
|    +-- NO  -> Check LeniencyTracker                       |
|              |-- breaches_this_month < 2? -> allow + log  |
|              +-- breaches_this_month >= 2? -> reject      |
+-----------------------------------------------------------+
```

The leniency tracker would store:

- `api_key_id`
- `month` (e.g., "2025-12")
- `breach_count`
- `breach_timestamps` (for auditing)

---

## Proposed Architecture

Given the requirements for **reusability across endpoints** and **varying limits per customer**, a layered approach is
recommended:

### Layer 1: Rate Limit Configuration (Database)

Store limit configurations per API key or customer tier:

```sql
CREATE TABLE rate_limit_config
(
    id                     UUID PRIMARY KEY,
    api_key_id             UUID      NOT NULL REFERENCES api_key (id),
    daily_limit            INT       NOT NULL,
    monthly_leniency_count INT       NOT NULL DEFAULT 2,
    effective_from         TIMESTAMP NOT NULL,
    version                INT       NOT NULL DEFAULT 0,
    UNIQUE (api_key_id)
);
```

### Layer 2: Bucket Management Service

A service that:

- Creates/caches bucket4j buckets per API key
- Listens for configuration changes and calls `replaceConfiguration()`
- Handles bucket recreation on app restart (from persisted config)

### Layer 3: Leniency Tracker (Database/Redis)

```sql
CREATE TABLE leniency_usage
(
    id             UUID PRIMARY KEY,
    api_key_id     UUID       NOT NULL REFERENCES api_key (id),
    year_month     VARCHAR(7) NOT NULL, -- e.g., "2025-12"
    breach_count   INT        NOT NULL DEFAULT 0,
    last_breach_at TIMESTAMP,
    UNIQUE (api_key_id, year_month)
);
```

### Layer 4: Unified Rate Limit Filter

A single filter that orchestrates all layers:

```
Request -> Extract API Key -> RateLimitService.tryConsume()
                                    |
                    +---------------+---------------+
                    v                               v
            bucket.tryConsume()          leniencyTracker.canBreach()
                    |                               |
                    +---------------+---------------+
                                    v
                            Allow / Reject
```

---

## Component Design

### RateLimitService Interface

```java
public interface RateLimitService {

    /**
     * Attempts to consume a token for the given API key.
     *
     * @return RateLimitResult containing success status, remaining tokens,
     *         and whether leniency was used
     */
    RateLimitResult tryConsume(UUID apiKeyId);

    /**
     * Updates the rate limit configuration for an API key.
     * Takes effect immediately.
     */
    void updateConfiguration(UUID apiKeyId, RateLimitConfig config);

    /**
     * Gets current usage statistics for an API key.
     */
    UsageStatistics getUsageStatistics(UUID apiKeyId);
}
```

### RateLimitResult Record

```java
public record RateLimitResult(boolean allowed, long remainingTokens, long resetAtEpochSecond, boolean leniencyUsed,
                              int leniencyRemaining) {
}
```

### Configuration Record

```java
public record RateLimitConfig(long dailyLimit, int monthlyLeniencyCount, TokensInheritanceStrategy upgradeStrategy) {
}
```

---

## Distribution Considerations

The current in-memory `ConcurrentHashMap` won't work across multiple instances. For production:

### Option A: bucket4j-redis (Recommended)

- Add `bucket4j-redis` with Lettuce or Redisson
- Buckets are distributed automatically
- Dependencies needed:
    - `com.bucket4j:bucket4j-redis`
    - `io.lettuce:lettuce-core` (or `org.redisson:redisson`)

### Option B: bucket4j-jcache + Redis

- Use JCache API with Redis provider (Redisson)
- More abstraction, same result

### Redis Key Structure

```
rate_limit:bucket:{api_key_id}     -> bucket4j state
rate_limit:leniency:{api_key_id}:{year_month} -> breach count
```

---

## Detailed Comparison: bucket4j-redis vs bucket4j-jcache

### Option A: bucket4j-redis (Direct Redis Integration)

Uses bucket4j's native Redis integration modules with a Redis client library (Lettuce, Redisson, or Jedis).

**Dependencies:**

```kotlin
implementation("com.bucket4j:bucket4j-redis")
implementation("io.lettuce:lettuce-core")  // OR org.redisson:redisson
```

#### Pros

| Advantage                 | Description                                                                                         |
|---------------------------|-----------------------------------------------------------------------------------------------------|
| **Async Support**         | Lettuce and Redisson support non-blocking async operations via `LettuceBasedProxyManager.asAsync()` |
| **Better Performance**    | No JCache abstraction overhead; direct Redis protocol communication                                 |
| **Redis Cluster Support** | All three clients (Lettuce, Redisson, Jedis) support Redis Cluster mode                             |
| **Auto-Expiration**       | Redis TTL can be used via `ExpirationAfterWriteStrategy` to automatically clean up stale buckets    |
| **Simpler Setup**         | Fewer moving parts; just Redis client + bucket4j-redis module                                       |
| **Active Development**    | bucket4j-redis is actively maintained with dedicated optimizations                                  |

#### Cons

| Disadvantage                | Description                                                                                  |
|-----------------------------|----------------------------------------------------------------------------------------------|
| **Redis Lock-in**           | Tied specifically to Redis; switching to another cache requires code changes                 |
| **Key Scanning Limitation** | Identifying all buckets requires scanning all Redis keys (no grouping mechanism like tables) |
| **Client Choice Matters**   | Jedis doesn't support async; must choose Lettuce or Redisson for async needs                 |
| **Limited Contributors**    | bucket4j is maintained by only two contributors (risk factor)                                |

#### Redis Client Sub-Options

| Client       | Async | Cluster | Notes                                                                      |
|--------------|-------|---------|----------------------------------------------------------------------------|
| **Lettuce**  | Yes   | Yes     | ~30% faster raw throughput; lower-level API; recommended for performance   |
| **Redisson** | Yes   | Yes     | Richer feature set (distributed locks, collections); easier high-level API |
| **Jedis**    | No    | Yes     | Simplest API; avoid if async is needed                                     |

**Recommendation:** Use **Lettuce** for best performance, or **Redisson** if you need additional distributed features
beyond rate limiting.

---

### Option B: bucket4j-jcache + Redis

Uses the JCache (JSR-107) specification as an abstraction layer, with Redisson as the JCache provider backed by Redis.

**Dependencies:**

```kotlin
implementation("com.bucket4j:bucket4j-jcache")
implementation("org.redisson:redisson")
implementation("javax.cache:cache-api")
```

#### Pros

| Advantage                   | Description                                                                  |
|-----------------------------|------------------------------------------------------------------------------|
| **Specification-Based**     | JCache (JSR-107) is a Java standard; code is portable across providers       |
| **Provider Flexibility**    | Can swap Redis for Hazelcast, Apache Ignite, Infinispan, or Oracle Coherence |
| **Familiar API**            | Standard `Cache<K,V>` interface familiar to Java developers                  |
| **Battle-Tested Providers** | Hazelcast, Ignite, Coherence are enterprise-grade with strong support        |

#### Cons

| Disadvantage               | Description                                                                                 |
|----------------------------|---------------------------------------------------------------------------------------------|
| **No Async Support**       | JCache API (JSR-107) does not specify async operations; always synchronous                  |
| **Redisson JCache Issues** | Redisson's JCache implementation is not fully spec-compliant; compatibility issues reported |
| **Abstraction Overhead**   | Additional layer between bucket4j and Redis adds latency                                    |
| **No Flexible Expiration** | JCache doesn't support per-entry TTL; bucket cleanup is less efficient                      |
| **No Thin-Client Support** | JCache lacks thin-client mode for memory-constrained environments                           |
| **Compatibility Risk**     | "Verification of compatibility with a particular JCache provider is your responsibility"    |

---

### Deployment Scenarios

#### Single Instance Deployment (1 customer-api)

| Aspect             | bucket4j-redis                  | bucket4j-jcache                   |
|--------------------|---------------------------------|-----------------------------------|
| **Necessity**      | Optional - in-memory works fine | Optional - in-memory works fine   |
| **Benefit**        | Bucket state survives restarts  | Bucket state survives restarts    |
| **Complexity**     | Low - just add Redis connection | Medium - JCache config + provider |
| **Recommendation** | Use if you want persistence     | Use only if already using JCache  |

**For single instance:** In-memory (`bucket4j-core`) is sufficient unless you need bucket state persistence across
restarts. If persistence is needed, **bucket4j-redis with Lettuce** is simpler.

#### Multi-Instance Deployment (> 1 customer-api)

| Aspect             | bucket4j-redis                                                                 | bucket4j-jcache                     |
|--------------------|--------------------------------------------------------------------------------|-------------------------------------|
| **Necessity**      | **Required** for shared state                                                  | **Required** for shared state       |
| **How It Works**   | All instances share buckets via Redis; token consumption synchronized globally | Same, via JCache abstraction        |
| **Consistency**    | Strong - Redis handles atomic operations                                       | Strong - depends on provider        |
| **Latency**        | Lower (direct Redis)                                                           | Higher (JCache abstraction)         |
| **Async Filters**  | Possible with Lettuce/Redisson                                                 | Not possible (JCache is sync-only)  |
| **Recommendation** | **Preferred choice**                                                           | Only if provider flexibility needed |

**For multi-instance:** **bucket4j-redis is strongly recommended.** The async support matters for high-throughput
scenarios, and the simpler architecture reduces failure points.

---

### Decision Matrix

| If you...                               | Choose                                 |
|-----------------------------------------|----------------------------------------|
| Need best performance                   | bucket4j-redis + Lettuce               |
| Need async/reactive support             | bucket4j-redis + Lettuce or Redisson   |
| Already use Redisson for other features | bucket4j-redis + Redisson              |
| Need to swap cache providers in future  | bucket4j-jcache (but accept sync-only) |
| Already use Hazelcast/Ignite            | bucket4j-jcache with that provider     |
| Run single instance, need simplicity    | bucket4j-core (in-memory)              |
| Run multiple instances                  | bucket4j-redis + Lettuce               |

---

### Final Recommendation

**For customer-api: Use `bucket4j-redis` with `Lettuce`**

Rationale:

1. Multi-instance deployments will require distributed state
2. Async support enables better throughput under load
3. Lettuce offers best performance (~30% faster than Redisson in benchmarks)
4. Simpler architecture than JCache abstraction
5. Spring Boot has excellent Lettuce integration (spring-data-redis uses Lettuce by default)
6. Redis is likely already in your stack or easy to add

```kotlin
// Recommended dependencies
implementation("com.bucket4j:bucket4j-redis")
implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
```

---

## Summary Table

| Requirement                 | bucket4j Support | Solution                 |
|-----------------------------|------------------|--------------------------|
| Daily reset at midnight     | Native           | `intervallyAligned()`    |
| On-the-fly limit changes    | Native           | `replaceConfiguration()` |
| 2x monthly breach allowance | **Not native**   | Custom LeniencyTracker   |
| Reusable across endpoints   | Filter pattern   | Single `RateLimitFilter` |
| Varying limits per customer | Config-driven    | Database + cache         |
| Multi-instance deployment   | Native           | `bucket4j-redis`         |

---

## Billing Integration

For usage-based billing ("more requests = more pay"), track actual consumption separately from rate limiting:

- **Consumption counter**: Source for billing (count ALL requests)
- **Rate limit bucket**: Enforcement mechanism (block excess requests)

Consider a separate `UsageTracker` that:

1. Increments on every request (regardless of rate limit status)
2. Stores daily/monthly aggregates
3. Feeds into billing system

---

## Implementation Phases

### Phase 1: Core Rate Limiting

- Migrate from per-minute to daily limits using `intervallyAligned()`
- Add database-backed configuration
- Implement `RateLimitService` interface

### Phase 2: Dynamic Configuration

- Add configuration change detection
- Implement `replaceConfiguration()` logic
- Add admin API for limit management

### Phase 3: Leniency System

- Create `LeniencyTracker` service
- Add leniency database table
- Integrate with rate limit decision flow

### Phase 4: Distribution

- Add Redis dependencies
- Migrate to `bucket4j-redis`
- Move leniency tracking to Redis

### Phase 5: Billing Integration

- Add usage tracking
- Create usage aggregation jobs
- Expose usage APIs for billing system

---

## References

- [Bucket4j 8.12.1 Reference](https://bucket4j.com/8.12.1/toc.html)
- [Bucket4j 8.10.1 Reference](https://bucket4j.com/8.10.1/toc.html)
- [Rate Limiting with Spring Boot, Bucket4j, and Redis - INNOQ](https://www.innoq.com/en/blog/2024/03/distributed-rate-limiting-with-spring-boot-and-redis/)
- [Rate Limiting a Spring API Using Bucket4j - Baeldung](https://www.baeldung.com/spring-bucket4j)
- [GitHub - bucket4j/bucket4j](https://github.com/bucket4j/bucket4j)
- [FreeCodeCamp - Rate Limiting with Bucket4j and Redis](https://www.freecodecamp.org/news/rate-limiting-with-bucket4j-and-redis/)
- [Feature Comparison: Redisson vs Lettuce - Redisson](https://redisson.pro/blog/feature-comparison-redisson-vs-lettuce.html)
- [Redisson vs Lettuce Performance Comparison - GitHub Issue](https://github.com/redisson/redisson/issues/3704)
