# Redis in API Gateway — Notes

## Where is Redis used?

Redis is used in **only one place** in this project — the `api-gateway`, specifically for **rate limiting**.

File: `src/main/java/com/easybuy/api_gateway/config/APIGatewayConfiguration.java`

```java
@Bean
public RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(
            10,  // replenishRate  — 10 tokens added per second
            15,  // burstCapacity  — bucket holds max 15 tokens
            5    // requestedTokens — each request costs 5 tokens
    );
}
```

---

## What is Rate Limiting?

Rate limiting controls **how many requests a user can make in a given time period**.

Without it, a single user (or attacker) could flood your API with thousands of requests per second, bringing down the entire system.

---

## How it works — Token Bucket Algorithm

Redis stores a "token bucket" for each user. Think of it like a bucket that:
- Starts with tokens in it
- Loses tokens as requests are made
- Refills tokens over time
- Blocks requests when the bucket is empty

```
replenishRate  = 10   → 10 tokens are added back every second
burstCapacity  = 15   → bucket can hold a maximum of 15 tokens at once
requestedTokens = 5   → each request costs 5 tokens
```

So a user can make:
- **3 requests/second** sustained (15 tokens ÷ 5 per request = 3 requests before empty, refills at 10/sec)
- **Burst of 3 requests** at once before hitting the limit

When the bucket is empty → API Gateway returns `429 Too Many Requests`.

---

## The full request flow

```
Request arrives at API Gateway
        ↓
KeyResolver extracts X-User-Id header
        ↓
Redis checks: does this user have enough tokens?
        ↓
Yes → deduct 5 tokens → allow request to pass through
No  → return 429 Too Many Requests immediately
```

---

## Why Redis specifically?

Redis is chosen over a regular database for three reasons:

### 1. Speed — In-memory storage
Checking and updating a token counter happens on **every single request**. A database query would add 10–50ms of latency per request. Redis operates in microseconds because everything is stored in RAM.

### 2. Shared state across instances
If you run **multiple instances** of the api-gateway (for load balancing), each instance needs to know the same rate limit state for a user. If each instance tracked limits locally, a user could bypass the limit by hitting different instances.

Redis is a **central shared store** — all gateway instances read and write to the same Redis, so limits are enforced globally.

```
Request → Gateway Instance 1 ─┐
                               ├──→ Redis (shared token bucket)
Request → Gateway Instance 2 ─┘
```

### 3. Atomic operations
When two requests from the same user arrive at the exact same millisecond, both instances try to decrement the token count simultaneously. Redis handles this with **atomic operations** — only one can decrement at a time, preventing race conditions where both requests sneak through.

---

## KeyResolver — how users are identified

```java
@Bean
public KeyResolver userIdKeyResolver() {
    return exchange -> {
        String userId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-USER-ID");
        return Mono.justOrEmpty(userId);
    };
}
```

The `KeyResolver` tells Redis **which bucket to use** for a given request. It reads the `X-User-Id` header:

- For **authenticated requests** → the header contains the user's UUID from the JWT token
- For **public requests** → the header contains the client's IP address (set by `AuthenticationFilter`)

This means each user/IP gets their own separate token bucket in Redis.

---

## Dependency used

```xml
<!-- api-gateway/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

The **reactive** variant is used because the API Gateway is built on Spring WebFlux (reactive/non-blocking), not Spring MVC.

---

## Where Redis is configured

Redis connection is configured in the git config repo (`easybuy-config`), not locally. It connects to the Redis container defined in the root `docker-compose.yml`:

```yaml
# docker-compose.yml
redis:
  image: redis:7.4
  container_name: redis-cache
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes
```

- Port `6379` is Redis's default port
- `--appendonly yes` enables persistence — Redis writes operations to disk so data survives container restarts

---

## Summary

| What | Why Redis |
|------|-----------|
| Rate limiting per user/IP | Needs to be fast (in-memory) |
| Works across multiple gateway instances | Needs shared central state |
| Concurrent requests handled correctly | Needs atomic increment/decrement |
| Token bucket state per user | Key-value store is a perfect fit |

Redis is **not used for caching** anywhere in this project yet — only for rate limiting in the API Gateway.
