# Kubernetes StatefulSets — Complete Notes

## 1. What is a StatefulSet?

A **StatefulSet** is a Kubernetes workload API object, similar to a Deployment, but designed for applications that need to maintain a persistent **identity** and **state** across restarts and rescheduling.

Where a Deployment treats all its pods as identical and disposable, a StatefulSet treats each pod as unique and durable.

---

## 2. Why We Need It (Deployment vs StatefulSet)

Deployments work great for **stateless** apps — web servers, REST APIs, frontends — where every pod does the same job and none of them "remember" anything. If a pod dies, Kubernetes just spins up a new one with a random name; nobody cares which one it is.

But some apps genuinely can't work that way:

- **Databases** (Postgres, MySQL, MongoDB) — need to know who the primary is, and need their own untouched storage.
- **Distributed systems** (Kafka, Zookeeper, Elasticsearch, Cassandra) — nodes need a stable identity to find and coordinate with each other.
- Some instances need to **start up in a specific order** (e.g., primary before replicas).

If you used a Deployment for these, pods would get new random names on every restart, and storage could get attached to the wrong instance — breaking clustering, replication, or quorum logic entirely.

### Analogy
- **Deployment** = interchangeable cashiers at a store. Any cashier can serve any customer.
- **StatefulSet** = doctors, each with their own patient files. Doctor #1 always comes back as Doctor #1 with Doctor #1's own files — identity and state must stay attached.

---

## 3. Deployment vs StatefulSet — Comparison Table

| Feature | Deployment | StatefulSet |
|---|---|---|
| Pod names | Random (`web-7f9d8-x2k1p`) | Predictable, numbered (`db-0`, `db-1`, `db-2`) |
| Pod identity | None — interchangeable | Stable, unique, persists across restarts |
| Storage | Shared/ephemeral, not tied to a pod | Each pod gets its own dedicated PersistentVolumeClaim |
| Startup/scale order | All at once, any order | Sequential, in order (0 → 1 → 2) |
| Networking | Random pod IP, load-balanced | Stable DNS name per pod via headless Service |
| Use case | Stateless apps | Stateful apps needing identity/order/storage |

---

## 4. How StatefulSet Works — The Three Guarantees

1. **Stable, unique network identity**
   Each pod gets a predictable name (`<statefulset-name>-<ordinal>`) and, when paired with a **headless Service**, a stable DNS hostname like `db-0.db`, `db-1.db`. This name stays the same across restarts/rescheduling.

2. **Stable, dedicated storage**
   Using `volumeClaimTemplates`, Kubernetes automatically creates a separate PersistentVolumeClaim (PVC) per pod (e.g., `data-db-0`, `data-db-1`). On restart, the same pod always reattaches to *its own* volume — never gets mixed up with another pod's data.

3. **Ordered, graceful deployment and scaling**
   Pods are created, updated, and deleted **one at a time, in order** (0, then 1, then 2...) by default. Scaling down happens in reverse order. This matters when pod-0 must be up before pod-1 can safely join a cluster.

---

## 5. How to Write One — Anatomy of a StatefulSet YAML

You need **two objects**: a headless Service (for stable DNS) + the StatefulSet itself.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: db
spec:
  clusterIP: None        # <-- headless: gives each pod its own DNS entry
  selector:
    app: postgres
  ports:
    - port: 5432
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: db
spec:
  serviceName: db          # must match the headless Service name
  replicas: 3
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:16
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:     # <-- creates one PVC per pod automatically
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
```

### Key fields explained
| Field | Purpose |
|---|---|
| `serviceName` | Links the StatefulSet to its headless Service for stable DNS |
| `replicas` | Number of pods (created/scaled in order) |
| `volumeClaimTemplates` | Defines the PVC template — one is created per pod, named `<template>-<statefulset>-<ordinal>` |
| `clusterIP: None` on Service | Makes the Service headless, so each pod gets its own resolvable DNS name instead of one shared load-balanced IP |

Resulting stable hostnames: `db-0.db.default.svc.cluster.local`, `db-1.db.default.svc.cluster.local`, etc.

---

## 6. Relation to Databases — Primary & Replicas

**Important:** Kubernetes/StatefulSet does **not** perform database replication itself. It only provides the stable identity + stable storage foundation. Replication is configured by the database engine on top of that foundation.

### How it fits together
- `db-0` is typically configured as the **primary** — it handles all writes.
- `db-1`, `db-2`, ... are **replicas** — they continuously copy data from the primary (streaming/async replication).
- When a client writes data via `db-0`, that data gets replicated to `db-1`/`db-2` within a short window. Reads served by a replica will reflect that data once replication catches up.
- Each pod's separate volume ends up holding a **copy of the same data**, not different/siloed data.

### Why StatefulSet is required for this to work
- **Stable network identity**: replication config on replicas points to a fixed hostname (`db-0.db`). If pod names were random (like a Deployment), replication links would break on every restart.
- **Stable storage**: if `db-1` restarted and accidentally attached to `db-2`'s volume, replication state would be corrupted. StatefulSet guarantees each pod always reattaches to its own disk.

### Manual setup vs Operators
Hand-writing replication logic (init scripts deciding "am I pod 0 → become primary, else → replicate from db-0") is fragile and rarely done manually in production.

**In real production**, teams use a **Kubernetes Operator** — a controller built for a specific database that automates:
- Primary election
- Replica configuration
- Automatic failover (if the primary dies, an operator promotes a replica)
- Backups, scaling, upgrades

| Database | Common Operator |
|---|---|
| PostgreSQL | CloudNativePG, Zalando Postgres Operator |
| MySQL | Percona XtraDB Cluster Operator, Vitess |
| MongoDB | MongoDB Community/Enterprise Operator |
| Redis | Redis Operator (Spotahome / Redis Enterprise) |

Example — CloudNativePG gives you a full 3-node HA Postgres cluster with one manifest:
```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: my-db
spec:
  instances: 3
  storage:
    size: 10Gi
```

### Scaling nuance
- **Scaling reads** (adding more replicas) is straightforward — more replicas = more read capacity.
- **Scaling writes** is *not* solved by just adding pods — a single primary is usually still the only writer. True write scaling requires **sharding** (tools like Vitess for MySQL, Citus for Postgres) — a much bigger architectural decision.

### Cloud equivalent: RDS
Managed services like AWS RDS solve the exact same problem but hide all the machinery:
- **Multi-AZ** = synchronous standby + automatic failover (equivalent to the operator's failure-detection/promotion logic)
- **Read replicas** = asynchronous copies for scaling reads (same concept as StatefulSet replicas)
- A stable **endpoint** replaces the stable pod DNS name — it always points to whichever instance is currently primary.

---

## 7. What Else You Should Use StatefulSet For (Beyond Databases)

Any system where instances need a stable identity, ordered startup, and/or their own durable storage:

| Category | Examples | Why StatefulSet |
|---|---|---|
| Relational/NoSQL databases | PostgreSQL, MySQL, MongoDB, Cassandra | Primary/replica identity, per-node storage |
| Message queues / streaming | Kafka, RabbitMQ (clustered), Pulsar | Brokers need stable identity for partition ownership and cluster membership |
| Coordination services | Zookeeper, etcd | Need stable identity for quorum/consensus (majority voting depends on knowing exactly which node is which) |
| Search/analytics clusters | Elasticsearch, Solr | Each node holds its own shard of data on disk |
| Caching layers (persistent) | Redis (clustered/persistent mode) | Node identity matters for cluster slot assignment |
| Monitoring/metrics storage | Prometheus (with persistent storage), Thanos, Cortex | Needs durable local storage tied to each instance |
| Distributed file/object storage | MinIO, Ceph | Each node owns and serves specific data shards |
| CI/CD or stateful custom apps | Jenkins (with persistent volumes), any app needing "sticky" local state | Needs the same disk back after every restart |

### Rule of thumb — use StatefulSet when your app needs ANY of:
- A pod to always come back with the **same name**
- A pod to always come back with the **same disk/data**
- Pods to start up or scale **in a specific order**
- Other pods/clients to reliably address **a specific instance** by name (not just "any instance")

### When you do NOT need it — stick with Deployment:
- Stateless web servers, REST APIs, frontend apps
- Any app where pods are fully interchangeable and disposable
- Apps with no local state, or where state lives entirely in an external managed service (e.g., app pods that just talk to RDS)

---

## 8. Quick Reference Cheatsheet

- `kubectl get pods` → StatefulSet pods show ordered names: `db-0`, `db-1`, `db-2`
- `kubectl get pvc` → one PVC per pod: `data-db-0`, `data-db-1`, `data-db-2`
- Scaling up/down (`kubectl scale statefulset db --replicas=5`) happens **in order**, one pod at a time
- Deleting a StatefulSet does **not** delete its PVCs by default — data persists unless you delete PVCs explicitly
- Headless Service (`clusterIP: None`) is required for per-pod stable DNS
- Kubernetes handles identity + storage; **the database/app itself handles replication logic**
- In production, prefer an **Operator** over hand-rolled replication scripts