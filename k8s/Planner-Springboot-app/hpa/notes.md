# Kubernetes Horizontal Pod Autoscaler (HPA) — Complete Guide

## ⚠️ First, an important clarification

You asked about "autoscaling nodes" via HPA — but these are actually **two different things** in Kubernetes:

| Concept | What it scales | Tool |
|---|---|---|
| **Horizontal Pod Autoscaler (HPA)** | Number of **Pods** in a Deployment/StatefulSet | Built into k8s |
| **Cluster Autoscaler (CA)** | Number of **Nodes** (VMs/machines) in the cluster | Cloud-provider specific (AWS/GKE/AKS) |
| **Vertical Pod Autoscaler (VPA)** | CPU/Memory **requests/limits** of a single pod | Add-on |

HPA does **not** add or remove nodes. It adds/removes **pod replicas**. If your existing nodes run out of capacity to schedule those new pods, *that's* when Cluster Autoscaler kicks in (in a real cloud cluster) to add nodes — and scales nodes down when they're underutilized.

Since **Docker Desktop's Kubernetes is a single-node cluster**, Cluster Autoscaler is **not applicable** locally (there are no additional nodes to add). So this guide focuses on what you *can* actually learn and test locally: **HPA (pod-level autoscaling)**. I've added a short section at the end explaining how node autoscaling works conceptually in production, since you'll need that for real prod clusters (EKS/GKE/AKS).

---

## 1. What is HPA?

The **Horizontal Pod Autoscaler** is a Kubernetes controller that automatically increases or decreases the **number of pod replicas** in a Deployment, ReplicaSet, or StatefulSet based on observed metrics — typically CPU utilization, memory utilization, or custom/external metrics (like requests-per-second, queue length, etc.).

It works as a **control loop**:

```
metrics-server / custom metrics API
            │
            ▼
   HPA Controller (checks every ~15s)
            │
            ▼
   Compares current metric vs target
            │
            ▼
   Adjusts replica count on Deployment
```

## 2. Why do we need it?

- **Handle variable traffic**: Scale out during peak load (e.g., flash sale, morning traffic spike) and scale in during idle periods.
- **Cost efficiency**: Running the max number of pods 24/7 wastes compute. HPA right-sizes replica count to actual demand.
- **Resilience**: Prevents a single overloaded pod from becoming a bottleneck or crashing under load.
- **Automation**: Removes the need for manual `kubectl scale` during traffic changes — critical for prod where you can't watch dashboards 24/7.
- **SLA/SLO compliance**: Keeps latency and error rates within target by adding capacity before things degrade.

## 3. How HPA works internally

1. HPA needs a **metrics source**. By default this is the **Metrics Server**, which collects CPU/memory usage from the kubelet on each node.
2. Every sync period (default 15s), the HPA controller queries current metric values for the target pods.
3. It computes the desired replica count using this formula (for resource metrics):

```
desiredReplicas = ceil( currentReplicas × ( currentMetricValue / desiredMetricValue ) )
```

Example: 4 replicas running at 80% CPU, target is 50% CPU →
`desiredReplicas = ceil(4 × (80/50)) = ceil(6.4) = 7`

4. It updates the Deployment's `spec.replicas` field accordingly (bounded by `minReplicas` / `maxReplicas`).
5. Scale-up is fast (default no delay), scale-down is intentionally slower (default stabilization window ~5 min) to avoid "flapping."

HPA can scale on:
- **Resource metrics**: CPU, memory (needs Metrics Server)
- **Custom metrics**: e.g. requests/sec from Prometheus (needs Prometheus Adapter)
- **External metrics**: e.g. SQS queue depth, Kafka lag (needs an external metrics adapter)

---

## 4. Prerequisites

### General (any cluster)
- A running Kubernetes cluster (v1.23+ recommended; HPA `autoscaling/v2` API is stable from 1.23+).
- `kubectl` installed and configured.
- **Metrics Server** installed (HPA has no data to act on without it, unless you're using only custom/external metrics).
- Your Deployment's pods **must define `resources.requests`** (especially CPU) — HPA calculates percentage utilization relative to the *requested* value, not the limit. Without requests set, CPU-based HPA won't work at all.

### Docker Desktop specific
- Docker Desktop with Kubernetes enabled: **Settings → Kubernetes → Enable Kubernetes**.
- Sufficient resources allocated to Docker Desktop (Settings → Resources): at least 2 CPUs / 4GB RAM recommended so you can actually observe pods scaling.
- Metrics Server needs a small patch to work on Docker Desktop because its self-signed kubelet certs aren't trusted by default — you'll add `--kubelet-insecure-tls` (shown below). This is fine for local dev; **do not** do this in real prod.
- A load-generation tool to trigger scaling — we'll use a simple `busybox` pod running a `wget` loop, no extra install needed.

---

## 5. Step-by-Step Implementation on Docker Desktop

### Step 1 — Enable Kubernetes & verify cluster

```bash
kubectl config use-context docker-desktop
kubectl get nodes
```

You should see a single node in `Ready` state.

### Step 2 — Install Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Docker Desktop's kubelet uses a self-signed certificate, so metrics-server will fail to scrape it out of the box. Patch it:

```bash
kubectl patch deployment metrics-server -n kube-system --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

Verify it's working (may take ~1 min to report data):

```bash
kubectl get deployment metrics-server -n kube-system
kubectl top nodes
kubectl top pods -A
```

If `kubectl top nodes` returns numbers (not an error), you're good.

### Step 3 — Deploy a sample app with resource requests

Create `deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: php-apache
spec:
  replicas: 1
  selector:
    matchLabels:
      run: php-apache
  template:
    metadata:
      labels:
        run: php-apache
    spec:
      containers:
      - name: php-apache
        image: registry.k8s.io/hpa-example
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: 200m
          limits:
            cpu: 500m
---
apiVersion: v1
kind: Service
metadata:
  name: php-apache
spec:
  selector:
    run: php-apache
  ports:
  - port: 80
```

Apply it:

```bash
kubectl apply -f deployment.yaml
kubectl get pods -w
```

### Step 4 — Create the HPA

**Option A — imperative (quick test):**

```bash
kubectl autoscale deployment php-apache --cpu-percent=50 --min=1 --max=5
```

**Option B — declarative YAML (recommended for prod, since it's version-controlled):**

`hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 1
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
```

**What each field means:**

| Field | Meaning |
|---|---|
| `apiVersion: autoscaling/v2` | Uses the stable `v2` HPA API (supports multiple metrics and `behavior`; `v1` only supports CPU). |
| `kind: HorizontalPodAutoscaler` | Declares this object as an HPA resource. |
| `metadata.name` | Name of this HPA object — used when you run `kubectl get hpa` / `kubectl describe hpa`. |
| `spec.scaleTargetRef` | Points the HPA at the workload it should scale — here, the Deployment named `php-apache`. Could also target a StatefulSet or ReplicaSet. |
| `spec.minReplicas` | The floor — HPA will never scale below this many pods, even at zero load. Keep ≥2 in prod for availability. |
| `spec.maxReplicas` | The ceiling — HPA will never scale above this many pods, protecting you from runaway scaling (e.g., due to a metrics bug or cost blowout). |
| `spec.metrics` | List of metrics HPA evaluates to decide scaling. Here we use one: CPU. |
| `metrics[].type: Resource` | Tells HPA this metric comes from the built-in resource metrics API (CPU/memory via Metrics Server), as opposed to `Pods`, `Object`, or `External` (used for custom/external metrics). |
| `resource.name: cpu` | The specific resource being measured — could also be `memory`. |
| `resource.target.type: Utilization` | Scale based on **percentage of the requested value** being used (vs `AverageValue`, which uses an absolute quantity like `500m`). |
| `resource.target.averageUtilization: 50` | The target: keep average CPU usage across all pods at 50% of their `requests.cpu`. Above this, scale up; below this, scale down. |
| `spec.behavior` | Optional fine-grained control over *how fast* scaling happens in each direction (without it, HPA uses built-in defaults). |
| `behavior.scaleDown.stabilizationWindowSeconds: 300` | Before scaling down, HPA looks back over the last 300s (5 min) of recommendations and picks the *highest* (safest) replica count seen in that window. This prevents rapid flapping when load briefly dips. |
| `behavior.scaleDown.policies` | Rules capping how much scale-down is allowed per step. |
| `policies[].type: Percent` / `value: 50` / `periodSeconds: 60` | In any 60-second window, replicas can shrink by at most 50% of the current count — a gradual, controlled ramp-down rather than an abrupt drop. |
| `behavior.scaleUp.stabilizationWindowSeconds: 0` | No delay before scaling up — react to load spikes immediately (the default and generally desired behavior, since under-provisioning hurts users more than over-provisioning costs money). |
| `behavior.scaleUp.policies` | Rules capping how much scale-up is allowed per step. |
| `policies[].type: Percent` / `value: 100` / `periodSeconds: 15` | In any 15-second window, replicas can at most double (100% increase) — fast enough to absorb spikes, but still bounded so a metrics glitch can't instantly blow past `maxReplicas`'s intent in one jump. |

```bash
kubectl apply -f hpa.yaml
kubectl get hpa
```

You should see something like:

```
NAME             REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS
php-apache-hpa   Deployment/php-apache   0%/50%    1         5         1
```

## 6. How Node Autoscaling Actually Works in Prod (context, not runnable locally)

Since Docker Desktop is single-node, you can't test this locally — but conceptually, in a real cloud cluster:

1. HPA scales pod replicas up.
2. The scheduler tries to place new pods on existing nodes.
3. If no node has enough free CPU/memory, new pods stay in `Pending` state.
4. **Cluster Autoscaler** watches for `Pending` pods caused by insufficient resources and requests new nodes from the cloud provider (e.g., adds an EC2 instance to an ASG on AWS, or a node pool VM on GKE/AKS).
5. When nodes are underutilized for a sustained period (pods could be rescheduled onto fewer nodes), CA cordons, drains, and terminates the extra nodes — scaling down.

So in production, the full autoscaling stack is typically: **HPA (pods) + Cluster Autoscaler (nodes)**, sometimes with **VPA (right-sizing individual pod resource requests)** used alongside for further optimization — though VPA and HPA on the same metric (CPU) can conflict, so that combination needs care.