# FeignClient + Eureka — How It All Works

## What is Eureka?

Eureka is a **Service Registry** from Netflix, integrated into Spring Cloud.

Every microservice on startup:
1. **Registers itself** with the Eureka server using its `spring.application.name`
2. **Sends heartbeats** every 30s to stay alive in the registry
3. **Fetches the registry** periodically so it knows where other services are

Eureka server keeps a map like:

```
INVENTORY-SERVICE  →  [192.168.1.10:8082, 192.168.1.11:8082]
USER-SERVICE       →  [192.168.1.12:8083]
PRODUCT-CATEGORY-SERVICE → [192.168.1.13:8084]
```

---

## Scenario 1 — Without Eureka (Hardcoded URL)

```java
@FeignClient(name = "inventory-client", url = "http://localhost:8082")
public interface InventoryClient {
    @GetMapping("/api/inventories/product/{productId}")
    InventorySnapshot getInventoryByProductId(@PathVariable UUID productId);
}
```

**How it works:**
- Feign sends every request directly to `http://localhost:8082`
- No service discovery involved
- `name` here is just a bean identifier, not used for routing

**Problems:**
- Hardcoded IP/port — breaks in any non-local environment
- No load balancing — if you run 2 instances of inventory-service, Feign always hits the same one
- You must update code when the address changes

**When to use:**
- Calling a third-party external API (e.g., payment gateway, SMS provider)
- Calling a service that is not registered in Eureka

---

## Scenario 2 — With Eureka Only (`name` only)

```java
@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {
    @GetMapping("/api/inventories/product/{productId}")
    InventorySnapshot getInventoryByProductId(@PathVariable UUID productId);
}
```

**How it works:**
1. Feign sees `name = "INVENTORY-SERVICE"` and no `url`
2. Spring Cloud LoadBalancer intercepts the request
3. It looks up `INVENTORY-SERVICE` in the Eureka registry
4. Gets back a list of live instances with their IPs and ports
5. Picks one (round-robin by default)
6. Sends the request to that instance

**Flow:**
```
cart-order-service
    → FeignClient("INVENTORY-SERVICE")
        → Spring Cloud LoadBalancer
            → Eureka: "where is INVENTORY-SERVICE?"
                → returns [192.168.1.10:8082]
            → sends request to http://192.168.1.10:8082/api/inventories/...
```

**Benefits:**
- No hardcoded IPs
- Automatic load balancing across multiple instances
- If an instance goes down, Eureka removes it and LoadBalancer skips it

**Requirement:**
- Both services must be registered with the same Eureka server
- `spring-cloud-starter-netflix-eureka-client` must be on classpath
- `eureka.client.service-url.defaultZone` must point to Eureka server

**This is what the project currently uses.**

---

## Scenario 3 — `name` + `url` Both (Flexible / Env-Driven)

```java
@FeignClient(name = "${INVENTORY_SERVICE_NAME}", url = "${INVENTORY_SERVICE_URL:}")
public interface InventoryClient {
    @GetMapping("/api/inventories/product/{productId}")
    InventorySnapshot getInventoryByProductId(@PathVariable UUID productId);
}
```

**How it works:**

| `INVENTORY_SERVICE_URL` value | Behavior |
|-------------------------------|----------|
| Empty string `""` or not set  | Feign ignores `url`, falls back to Eureka via `name` |
| `http://inventory-service:8082` | Feign uses this URL directly, bypasses Eureka |

**The `:` default syntax:**
```
${INVENTORY_SERVICE_URL:}   →  defaults to "" if variable is missing
${INVENTORY_SERVICE_URL}    →  throws IllegalArgumentException if variable is missing
```

**application.yml for local dev (Eureka mode):**
```yaml
INVENTORY_SERVICE_NAME: INVENTORY-SERVICE
INVENTORY_SERVICE_URL: ""          # empty = use Eureka

USER_SERVICE_NAME: USER-SERVICE
USER_SERVICE_URL: ""

PRODUCT_SERVICE_NAME: PRODUCT-CATEGORY-SERVICE
PRODUCT_SERVICE_URL: ""
```

**application.yml for production/K8s (direct URL mode):**
```yaml
INVENTORY_SERVICE_NAME: inventory-client   # just a bean name, not used for routing
INVENTORY_SERVICE_URL: http://inventory-service:8082

USER_SERVICE_NAME: user-client
USER_SERVICE_URL: http://user-service:8083

PRODUCT_SERVICE_NAME: product-client
PRODUCT_SERVICE_URL: http://product-category-service:8084
```

---

## Scenario 4 — Kubernetes (No Eureka Needed)

In Kubernetes, Eureka becomes redundant because K8s has its own built-in service discovery via **DNS**.

**How K8s service discovery works:**
- Every K8s `Service` object gets a DNS name automatically
- Format: `<service-name>.<namespace>.svc.cluster.local`
- Short form within same namespace: just `<service-name>`

So `inventory-service` deployed as a K8s Service is reachable at:
```
http://inventory-service:8082
```

K8s handles:
- Load balancing across pods (via kube-proxy)
- Health checks (via liveness/readiness probes)
- Automatic DNS updates when pods restart

**FeignClient in K8s:**
```java
@FeignClient(name = "${INVENTORY_SERVICE_NAME}", url = "${INVENTORY_SERVICE_URL:}")
```

```yaml
# K8s environment variables (set in Deployment manifest or ConfigMap)
INVENTORY_SERVICE_NAME: inventory-client
INVENTORY_SERVICE_URL: http://inventory-service:8082
```

You don't run Eureka at all in K8s. The `url` env var points to the K8s Service DNS name.

**K8s Deployment env section:**
```yaml
env:
  - name: INVENTORY_SERVICE_URL
    value: "http://inventory-service:8082"
  - name: USER_SERVICE_URL
    value: "http://user-service:8083"
  - name: PRODUCT_SERVICE_URL
    value: "http://product-category-service:8084"
```

---

## Summary Table

| Scenario | `name` | `url` | Discovery | Load Balancing |
|----------|--------|-------|-----------|----------------|
| External API | bean id | hardcoded | None | None |
| Local with Eureka | service name in Eureka | absent/empty | Eureka | Spring Cloud LB |
| Flexible (env-driven) | env var | env var (empty = Eureka) | Eureka or direct | Eureka LB or K8s |
| Kubernetes | bean id | K8s DNS name | K8s DNS | kube-proxy |

---

## This Project's Clients

| Client | Current `name` | Eureka Registration Name |
|--------|---------------|--------------------------|
| `InventoryClient` | `${INVENTORY_SERVICE_NAME}` | `INVENTORY-SERVICE` |
| `UserClient` | `${USER_SERVICE_NAME}` | `USER-SERVICE` |
| `ProductClient` | `${PRODUCT_SERVICE_NAME}` | `PRODUCT-CATEGORY-SERVICE` |

All three use Scenario 3 (flexible) — Eureka locally, direct URL in K8s — just by changing env vars.

---

## What Happens If Eureka Is Down?

- Feign clients that rely on Eureka will fail with `No instances available for SERVICE-NAME`
- Spring Cloud LoadBalancer caches the registry for a short time, so brief Eureka downtime may still work
- For resilience, add a fallback using `@FeignClient(fallback = InventoryClientFallback.class)`

```java
@Component
public class InventoryClientFallback implements InventoryClient {
    @Override
    public InventorySnapshot getInventoryByProductId(UUID productId) {
        // return a safe default or throw a business exception
        throw new ServiceUnavailableException("Inventory service is currently unavailable");
    }
}
```
