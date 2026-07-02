# Microservices Observability & Monitoring — Learning Notes

> A running document of configs explained simply, built config-by-config.

---

## 1. `application.yml` — Shared Base Config

This file is the **common config inherited by all microservices**. Any setting here applies to every service unless a service-specific file overrides it.

---

### `metrics` block

```yaml
metrics:
  tags:
    application: ${spring.application.name}
```

#### What is a tag?
A **tag** is a label that gets attached to every metric your service produces.

Since you have many microservices, when metrics flow into a monitoring tool (like Prometheus or Grafana), you need to know **which service the metric came from**. The tag `application: ${spring.application.name}` automatically stamps every metric with the service's own name (e.g. `order-service`, `payment-service`).

> `${spring.application.name}` is a Spring placeholder — it gets replaced at runtime with the value of `spring.application.name` defined in each service's own config.

**Why you need it:** Without this tag, all metrics from all services look identical in your dashboard. You wouldn't know which service has high CPU, which is throwing errors, etc.

---

### `logging` block

```yaml
logging:
  level:
    root: INFO
  file:
    name: logs/${spring.application.name}.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} : %msg%n"
    console: "%d{HH:mm:ss} %-5level %logger{36} : %msg%n"
```

#### `logging.level.root: INFO`
Controls the **loudness** of your logs. The levels from most to least verbose are:

```
TRACE → DEBUG → INFO → WARN → ERROR
```

Setting `root: INFO` means:
- ✅ Log `INFO`, `WARN`, `ERROR` messages
- ❌ Skip `DEBUG` and `TRACE` (too noisy for production)

`root` means this applies to **every class** in your application.

---

#### `logging.file.name`
By default, logs only appear in the **console/terminal**. This setting tells Spring Boot to **also write logs to a file**.

`logs/${spring.application.name}.log` → each service gets its own file:
- `logs/order-service.log`
- `logs/payment-service.log`
- etc.

This is essential for debugging issues in production where you can't watch the console live.

---

#### `logging.pattern.file`
The **format** of each line written to the log file.

```
%d{yyyy-MM-dd HH:mm:ss}   → Full timestamp:     2024-01-15 14:32:01
%-5level                   → Log level (padded): INFO , WARN , ERROR
[%thread]                  → Thread name:        [http-nio-8080-exec-1]
%logger{36}                → Class name:         com.example.OrderService
%msg                       → The log message:    Order created: #1234
%n                         → New line
```

**Example log line:**
```
2024-01-15 14:32:01 INFO  [http-nio-8080-exec-1] c.example.OrderService : Order created: #1234
```

---

#### `logging.pattern.console`
Same idea as `file` pattern, but **shorter** — used for what you see in the terminal during development.

```
%d{HH:mm:ss}   → Time only (no date):  14:32:01
%-5level        → Log level:            INFO
%logger{36}     → Class name
%msg            → The message
%n              → New line
```

Shorter because the console is for quick human reading — you don't need the full date when you're watching logs scroll by live.

---

## 2. Docker Compose + Tool Configs — The Full Observability Stack

### The big picture

There are two types of data to observe, and they travel through two separate pipelines:

| Pipeline | Data type | Flow |
|----------|-----------|------|
| Metrics  | Numbers over time (CPU, requests/sec, memory) | Spring Boot → Prometheus → Grafana |
| Logs     | Text messages your app writes | Spring Boot → log file → Alloy → Loki → Grafana |

---

### Docker Compose — boots all 4 tools

```yaml
services:
  prometheus: ...
  loki: ...
  grafana: ...
  alloy: ...
```

All containers share the same Docker internal network. This means they can reach each other using just the **service name** as a hostname. For example, `loki:3100` means "the container named loki, port 3100." No IP addresses needed.

---

## METRICS pipeline

### How it works
Prometheus is a **puller** — every 15 seconds it visits each service's `/actuator/prometheus` URL and reads the metrics. Your Spring Boot app just needs to expose that URL (Spring Actuator does this automatically).

### Prometheus config (`prometheus/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s       # poll every service every 15 seconds
  evaluation_interval: 15s   # re-check alert rules every 15 seconds
```

```yaml
scrape_configs:
  - job_name: prometheus             # Prometheus monitors itself
    static_configs:
      - targets:
          - prometheus:9090

  - job_name: users-service          # your Spring Boot app
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - host.docker.internal:8085  # special hostname = your laptop
```

> `host.docker.internal` is Docker's special hostname for "the machine running Docker." Your Spring Boot app runs on your laptop (outside Docker), so this is how Prometheus reaches it.

---

## LOGS pipeline

### Step 1 — Alloy collects log files

Alloy watches log files on disk and ships them to Loki.

The volume mount in docker-compose is what connects your laptop's log files into Alloy's container:
```yaml
volumes:
  - ../logs:/workspace   # your laptop's ../logs/ folder = /workspace inside Alloy
```

#### Alloy config (`alloy/config.alloy`)

```
local.file_match "spring_logs" {
  path_targets = [{ __path__ = "/workspace/*.log", environment = "dev" }]
}
```
Watch every `.log` file in `/workspace/` (which is your `../logs/` folder on your laptop).

```
loki.source.file "spring_logs" {
  targets    = local.file_match.spring_logs.targets
  forward_to = [loki.process.labels.receiver]
}
```
Read those files line by line, send each line to the label processor.

```
loki.process "labels" {
  stage.regex {
    source     = "filename"
    expression = ".*/(?P<service>[^/]+)\.log"
  }
  stage.labels {
    values = { service = "service" }
  }
  forward_to = [loki.write.default.receiver]
}
```
Extract the service name from the filename (`users-service.log` → `service=users-service`) and attach it as a label to every log line.

```
loki.write "default" {
  endpoint { url = "http://loki:3100/loki/api/v1/push" }
}
```
Push the labeled log lines to Loki over HTTP.

---

### Step 2 — Loki stores logs

Loki is a log database — it stores log lines indexed by time and labels (not by full text).

### Step 3 — Grafana shows everything

After starting, go to `http://localhost:3000`, log in with admin/admin.

---

### Port reference

| Service    | Port  | What it's for                          |
|------------|-------|----------------------------------------|
| Grafana    | 3000  | Dashboard UI — open in your browser    |
| Loki       | 3100  | Receives log pushes from Alloy         |
| Prometheus | 9090  | Metrics storage + UI                   |
| Alloy      | 12345 | Alloy's own management UI              |
| Tempo      | 3200  | Tempo HTTP API + Grafana data source   |
| Tempo      | 4317  | gRPC — receives traces from OTel agents|

---

## 3. Line-by-line breakdown of every config

### `docker-compose.yml` — line by line

#### Prometheus service

```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"
  command:
    - --config.file=/etc/prometheus/prometheus.yml
    - --web.enable-remote-write-receiver
  volumes:
    - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
```

- `--web.enable-remote-write-receiver` — enables Prometheus to accept metrics pushed from Tempo's metrics_generator

#### Loki service

```yaml
loki:
  image: grafana/loki:latest
  ports:
    - "3100:3100"
  command: -config.file=/etc/loki/config.yml
  volumes:
    - ./loki/config.yml:/etc/loki/config.yml
```

#### Grafana service

```yaml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin
  volumes:
    - ./grafana/provisioning:/etc/grafana/provisioning
  depends_on:
    - loki
    - prometheus
    - tempo
```

#### Alloy service

```yaml
alloy:
  image: grafana/alloy:latest
  ports:
    - "12345:12345"
  command: run /etc/alloy/config.alloy
  volumes:
    - ./alloy/config.alloy:/etc/alloy/config.alloy
    - /var/log:/var/log
    - ../logs:/workspace
```

#### Tempo service

```yaml
tempo:
  image: grafana/tempo:2.4.0
  container_name: tempo
  command:
    - --config.file=/etc/tempo/tempo.yaml
  volumes:
    - ./tempo/tempo.yaml:/etc/tempo/tempo.yaml
  ports:
    - "3200:3200"
    - "4317:4317"
    - "4318:4318"
```

---

## 4. Micrometer — why you need it and what problem it solves

### The problem with plain `/actuator/metrics`

Spring Boot exposes a built-in `/actuator/metrics` endpoint out of the box — no extra dependency needed. But it returns data in **Spring's own JSON format** which Prometheus does NOT understand.

Prometheus expects its own specific text format:

```
# HELP jvm_memory_used_bytes JVM memory used
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 1234567
```

### What Micrometer does

Micrometer is a **metrics translation layer**. It sits inside your Spring Boot app and translates Spring's internal metrics into whatever format your monitoring tool expects.

```
Your Spring Boot app
       ↓ (internal metrics)
   Micrometer
       ↓ (translates to Prometheus format)
  /actuator/prometheus
       ↓ (Prometheus scrapes this)
   Prometheus
```

### The dependency to add

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### The `application.yml` config you also need

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

---

## 5. Distributed Tracing — OpenTelemetry + Grafana Tempo

### What is tracing and why do you need it?

Metrics and logs cannot answer:
> "A user clicked checkout. The request went through order-service → cart-service → user-service. The whole thing took 3.2 seconds — which step was slow?"

That is what **distributed tracing** answers.

### Three core concepts: Trace, Span, TraceId

```
User clicks checkout
│
└── Trace (the entire journey, end to end)
       │
       ├── Span 1: order-service receives request        [0ms → 120ms]
       │      │
       │      ├── Span 2: order-service → cart-service   [10ms → 80ms]
       │      │      │
       │      │      └── Span 3: cart-service → user-service  [20ms → 60ms]
       │      │
       │      └── Span 4: order-service writes to DB     [85ms → 115ms]
```

### The full pipeline

```
Your Spring Boot app (on laptop)
       ↓  OTel Java Agent intercepts every call automatically
       ↓  Sends spans via OTLP/gRPC to :4317
Tempo (Docker container)
       ↓  Stores traces indexed by traceId
Grafana
       ↓  Queries Tempo by traceId
       ↓  Also searches Loki logs by same traceId
Dashboard — Logs + Metrics + Traces all linked together
```

### JVM options explained line by line

```bash
-javaagent:"/Users/.../monitoring/opentelemetry-javaagent.jar"
```
Tells the JVM to load the OTel agent before running your app. The agent instruments all libraries automatically — zero code changes needed.

```bash
-Dotel.service.name=cart-order-service
```
The name this service appears as in Grafana's trace view. Set differently for each microservice.

```bash
-Dotel.exporter.otlp.endpoint=http://localhost:4317
```
Where to send trace data. Port `4317` is Tempo's gRPC port. `localhost` because Spring Boot runs outside Docker.

```bash
-Dotel.exporter.otlp.protocol=grpc
```
Use gRPC as the transport protocol. Port 4317 is the standard gRPC port.

```bash
-Dotel.traces.exporter=otlp
```
Send traces in OTLP format — the format Tempo understands natively.

```bash
-Dotel.metrics.exporter=none
-Dotel.logs.exporter=none
```
Already handling metrics via Micrometer and logs via Alloy — tell OTel agent to only handle traces.

```bash
-Dotel.javaagent.debug=true
```
Print verbose debug output. Turn off once things work — generates a lot of noise.

```bash
-Dotel.resource.attributes=deployment.environment=local,service.version=1.0
```
Extra labels on every span — filter by environment or version in Grafana.

---

## 6. Grafana Datasource Provisioning

### What is provisioning?

Instead of manually adding datasources in the Grafana UI every time you recreate the container, provisioning auto-configures them on startup via a YAML file at:

```
grafana/provisioning/datasources/datasources.yml
```

### `datasources.yml`

```yaml
apiVersion: 1

datasources:
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    isDefault: false
    jsonData:
      httpMethod: GET
      tracesToLogsV2:
        datasourceUid: loki        # links traces → logs in Grafana Explore
      serviceMap:
        datasourceUid: prometheus  # pulls service graph metrics from Prometheus
      nodeGraph:
        enabled: true              # shows service dependency graph

  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: false
```

- `access: proxy` — Grafana server fetches data, not the browser
- `url` uses container hostnames — works because all containers share the same Docker network
- `tracesToLogsV2` — when you click a trace in Tempo, Grafana automatically jumps to the related logs in Loki using the same traceId
- `serviceMap` — pulls service graph metrics from Prometheus to show a dependency map

---

## 7. Problems Faced & How They Were Resolved

---

### Problem 1 — Tempo failed to start with `field ingester not found`

**Error:**
```
tempo | failed parsing config: failed to parse configFile /etc/tempo/tempo.yaml: yaml: unmarshal errors:
tempo |   line 13: field ingester not found in type app.Config
tempo |   line 16: field compactor not found in type app.Config
```

**Cause:**
The `docker-compose.yml` was using `image: grafana/tempo:latest` which pulled Tempo v3.0.0. The config file used the old Tempo v2.x structure where `ingester` and `compactor` were top-level fields. In Tempo v3, these fields were moved/removed.

**Resolution:**
Instead of rewriting the config file, we pinned the Tempo image to a version compatible with the existing config:

```yaml
# docker-compose.yml
tempo:
  image: grafana/tempo:2.4.0  # pinned — compatible with ingester/compactor as top-level fields
```

**Lesson:** Never use `latest` for images in a project — always pin to a specific version to avoid breaking config changes between major versions.

---

### Problem 2 — Grafana could not connect to Tempo

**Error in Grafana UI:**
```
Failed to connect to Tempo
```

**Root causes (two separate issues):**

**Issue A — Missing shared Docker network**

The `docker-compose.yml` had no `networks` definition. Without a shared network, containers cannot resolve each other by hostname. Grafana was trying to reach `http://tempo:3200` but Docker couldn't resolve `tempo` as a hostname.

**Fix:**
```yaml
networks:
  monitoring:
    driver: bridge

services:
  tempo:
    networks:
      - monitoring
  grafana:
    networks:
      - monitoring
  # ... all services get networks: - monitoring
```

**Issue B — Tempo's Query Frontend not ready on ARM64 (Apple Silicon)**

Even after fixing the network, Tempo reported:
```
Query Frontend not ready: not ready: number of queriers connected to query-frontend is 0
```

This is a known bug in Tempo 2.4.0 on ARM64 where the querier's internal gRPC worker cannot connect to the query frontend, causing Tempo to never reach `ready` state.

**Fix — disable the frontend worker in the querier:**
```yaml
# tempo/tempo.yaml
querier:
  frontend_worker:
    frontend_address: ""
```

Setting `frontend_address` to empty tells the querier to handle queries directly without routing through the frontend component.

---

### Problem 3 — `prometheus.yml` only had `user-service` and wrong port

**Issue:**
The original `prometheus.yml` only scraped `user-service` and pointed to port `8085` while user-service actually runs on `8083`. All other services were missing.

**Fix:**
Added all services with correct ports and also added a Tempo scrape job:

```yaml
scrape_configs:
  - job_name: user-service
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: [host.docker.internal:8083]

  - job_name: api-gateway
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: [host.docker.internal:8080]

  # ... all other services

  - job_name: tempo
    metrics_path: /metrics
    static_configs:
      - targets: [tempo:3200]   # tempo runs inside Docker so use container name
```

---

### Problem 4 — `metrics_generator` in `tempo.yaml` was incomplete

**Issue:**
The original `tempo.yaml` had `metrics_generator` defined but missing the `storage` block. This caused Tempo to silently fail to generate span metrics (RED metrics — Rate, Errors, Duration).

**Fix:**
```yaml
metrics_generator:
  registry:
    external_labels:
      source: tempo
      cluster: local
  storage:
    path: /tmp/tempo/generator/wal
    remote_write:
      - url: http://prometheus:9090/api/v1/write
        send_exemplars: true

overrides:
  defaults:
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics   # generates RED metrics per service automatically
```

Also needed to enable remote write receiver in Prometheus so Tempo can push into it:
```yaml
# prometheus command in docker-compose.yml
command:
  - --config.file=/etc/prometheus/prometheus.yml
  - --web.enable-remote-write-receiver
```

---

### Problem 5 — `grafana.depends_on` only waited for Loki

**Issue:**
Grafana's `depends_on` only listed `loki`. If Prometheus or Tempo hadn't started yet, Grafana's provisioned datasources would fail to connect on boot.

**Fix:**
```yaml
grafana:
  depends_on:
    - loki
    - prometheus
    - tempo
```

---

### Problem 6 — `@CreationTimestamp` on `ExceptionResponse` caused `timestamp: null`

**Issue:**
The `ExceptionResponse` POJO had `@CreationTimestamp` on the `timestamp` field. This annotation only works on JPA entity fields managed by Hibernate. On a plain POJO it does nothing — so `timestamp` was always `null` in the error response.

**Fix:**
Removed `@CreationTimestamp` and set the timestamp manually in `GlobalExceptionHandler.buildResponse()`:
```java
.timestamp(LocalDateTime.now())
```

---

### Problem 7 — `springSecurityFilterChain` bean name conflict

**Error:**
```
The bean 'springSecurityFilterChain' could not be registered.
A bean with that name has already been defined in WebSecurityConfiguration
```

**Cause:**
The method in `SecurityConfig` was named `springSecurityFilterChain` which is the same name Spring Security uses internally in `WebSecurityConfiguration`.

**Fix:**
Renamed the method:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
```

---

### Problem 8 — `AuthenticationConfiguration` bean not found

**Error:**
```
Parameter 0 of method authenticationManager required a bean of type
'AuthenticationConfiguration' that could not be found.
```

**Cause:**
`SecurityConfig` was missing `@EnableWebSecurity`. Without it, Spring Security's full auto-configuration doesn't run, so `AuthenticationConfiguration` is never registered as a bean.

**Fix:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

---

### Problem 9 — `CartOrderService` VM options had wrong format

**Issue:**
The existing VM options in `CartOrderService.xml` had backslashes and was missing the `-` before `javaagent`:
```
javaagent:"..." \ -Dotel.service.name=...
```

**Fix:**
Corrected to proper single-line format:
```
-javaagent:"..." -Dotel.service.name=...
```
And applied the same correct format to all 11 microservice run configurations.

---

## 8. New Things Added to the Project

| What | Where | Why |
|------|-------|-----|
| Shared `monitoring` Docker network | `docker-compose.yml` | Allows all containers to resolve each other by hostname |
| `container_name` for all services | `docker-compose.yml` | Predictable names for `docker exec` and hostname resolution |
| `grafana/provisioning/datasources/datasources.yml` | New file | Auto-configures Tempo, Prometheus, Loki datasources on Grafana startup — no manual UI setup needed |
| Tempo scrape job | `prometheus.yml` | Prometheus now collects Tempo's own internal metrics |
| `--web.enable-remote-write-receiver` | `prometheus.yml` command | Allows Tempo's metrics_generator to push span metrics into Prometheus |
| `metrics_generator.storage` block | `tempo.yaml` | Fixed silent failure — Tempo can now generate RED metrics per service |
| `service-graphs` + `span-metrics` processors | `tempo.yaml` | Generates Rate/Error/Duration metrics per service automatically |
| `querier.frontend_worker.frontend_address: ""` | `tempo.yaml` | Fixes ARM64 (Apple Silicon) Tempo query frontend bug |
| `grafana.depends_on` for all datasources | `docker-compose.yml` | Grafana waits for Tempo and Prometheus before starting |
| OTel VM options for all 11 services | `.idea/runConfigurations/*.xml` | Every service now sends traces to Tempo automatically |
| Tempo image pinned to `2.4.0` | `docker-compose.yml` | Prevents breaking config changes from `latest` pulling Tempo v3 |

---

*More configs will be added here as we go through them.*
