# Kubernetes Deployment - Summary Notes

## What is a Deployment?

A **Deployment** is a Kubernetes object that manages the lifecycle of Pods.

Instead of creating Pods directly, we create a Deployment, and Kubernetes ensures the desired number of Pods are always running.

```text
Deployment
      │
      ▼
 ReplicaSet
      │
      ▼
Pods
```

A Deployment itself **does not create Pods directly**. It creates a **ReplicaSet**, and the ReplicaSet creates and manages the Pods.

---

# Why do we need a Deployment?

Suppose we create a Pod directly.

```text
Pod
```

If the Pod crashes:

```text
❌ Pod Gone
```

Nobody recreates it.

You have to manually execute:

```bash
kubectl apply -f pod.yaml
```

Again.

---

With Deployment:

```text
Deployment
      │
      ▼
ReplicaSet
      │
      ▼
Pod
```

If the Pod dies:

```text
Deployment
      │
      ▼
ReplicaSet

Expected Pods = 1
Actual Pods = 0
```

ReplicaSet immediately creates another Pod.

This feature is called:

> **Self Healing**

---

# Responsibilities of Deployment

A Deployment is responsible for:

* Creating Pods
* Maintaining desired replicas
* Recreating failed Pods
* Scaling Pods
* Rolling Updates
* Rollbacks
* Managing ReplicaSets

---

# Deployment Architecture

```text
Deployment
      │
      ▼
ReplicaSet
      │
      ├────────► Pod
      ├────────► Pod
      └────────► Pod
```

---

# Anatomy of a Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment

metadata:
  name: nginx-deployment

spec:
  replicas: 3

  selector:
    matchLabels:
      app: nginx

  template:

    metadata:
      labels:
        app: nginx

    spec:
      containers:
      - name: nginx
        image: nginx:latest
```

---

# Important Sections

## 1. replicas

```yaml
replicas: 3
```

Means:

> I always want **3 Pods**.

If one crashes:

```
3 → 2
```

ReplicaSet creates another one.

Back to:

```
3 Pods
```

---

## 2. selector

```yaml
selector:
  matchLabels:
    app: nginx
```

This tells the Deployment:

> "Manage every Pod having"

```yaml
app: nginx
```

---

## 3. template

This is the blueprint of every Pod.

```yaml
template:
```

Inside it you define

* labels
* containers
* image
* ports
* resources

Basically,

> "Whenever you need a Pod, create it exactly like this."

---

# Labels

Example

```yaml
labels:
    app: nginx
```

Labels are simply key-value pairs.

Example

```text
app=nginx
env=dev
version=v1
team=backend
```

Labels help Kubernetes identify resources.

---

# Relationship between selector and labels

Deployment:

```yaml
selector:
    matchLabels:
        app: nginx
```

Pod Template

```yaml
labels:
    app: nginx
```

These **must match**.

Otherwise Deployment cannot manage the Pods.

---

# How Deployment creates Pods

You execute

```bash
kubectl apply -f deployment.yaml
```

↓

Deployment created

↓

Deployment creates ReplicaSet

↓

ReplicaSet creates Pods

↓

Pods start containers

---

# Verify Deployment

See Deployments

```bash
kubectl get deployments
```

---

See ReplicaSets

```bash
kubectl get replicasets
```

---

See Pods

```bash
kubectl get pods
```

---

Everything

```bash
kubectl get all
```

---

# Describe Deployment

```bash
kubectl describe deployment nginx-deployment
```

Shows

* Events
* Replica count
* Image
* Strategy
* Labels

Very useful while debugging.

---

# Scaling

Current

```text
3 Pods
```

Increase

```bash
kubectl scale deployment nginx-deployment --replicas=5
```

Now

```text
5 Pods
```

Decrease

```bash
kubectl scale deployment nginx-deployment --replicas=2
```

Pods are deleted automatically.

---

# Rolling Update

Suppose image changes

```
nginx:1.26
```

↓

```
nginx:1.27
```

Instead of stopping all Pods,

Kubernetes does

```
Old Pod
↓

New Pod Ready

↓

Delete Old Pod
```

Then repeats.

Users experience almost no downtime.

---

Update image

```bash
kubectl set image deployment/nginx-deployment nginx=nginx:1.27
```

Check rollout

```bash
kubectl rollout status deployment/nginx-deployment
```

History

```bash
kubectl rollout history deployment/nginx-deployment
```

Rollback

```bash
kubectl rollout undo deployment/nginx-deployment
```

---

# Self Healing Demo

Delete one Pod

```bash
kubectl delete pod nginx-deployment-xxxxx
```

Immediately

ReplicaSet creates another Pod.

No manual intervention required.

---

# Delete Deployment

```bash
kubectl delete deployment nginx-deployment
```

Everything created by Deployment is removed

* ReplicaSet
* Pods

---

# Deployment vs Pod

| Pod                         | Deployment                    |
| --------------------------- | ----------------------------- |
| Creates one Pod             | Creates and manages many Pods |
| No self-healing             | Self-healing                  |
| No scaling                  | Supports scaling              |
| No rolling update           | Rolling updates               |
| No rollback                 | Rollback support              |
| Mostly for learning/testing | Used in production            |

---

# Interview Questions

### Why not create Pods directly?

Pods are **ephemeral**. If they fail or are deleted, Kubernetes does not recreate them automatically. A Deployment ensures the desired number of Pods are always running by managing them through a ReplicaSet.

---

### What creates Pods in Kubernetes?

Technically:

```
Deployment
      │
      ▼
ReplicaSet
      │
      ▼
Pods
```

The **ReplicaSet** creates the Pods. The Deployment manages the ReplicaSet.

---

### Can we create a ReplicaSet directly?

Yes.

Should we?

Generally, **no**. In almost all real-world applications, you create a Deployment and let it manage the ReplicaSet for you.

---

### What is the difference between Deployment and ReplicaSet?

| Deployment               | ReplicaSet                       |
| ------------------------ | -------------------------------- |
| Higher-level controller  | Lower-level controller           |
| Manages ReplicaSets      | Manages Pods                     |
| Supports rolling updates | Does not support rolling updates |
| Supports rollbacks       | No rollback capability           |
| Used in production       | Usually managed by a Deployment  |

---

# Most Common Commands

```bash
# Create deployment
kubectl apply -f deployment.yaml

# View deployments
kubectl get deployments

# View ReplicaSets
kubectl get replicasets

# View Pods
kubectl get pods

# View all resources
kubectl get all

# Describe deployment
kubectl describe deployment nginx-deployment

# Scale deployment
kubectl scale deployment nginx-deployment --replicas=5

# Update image
kubectl set image deployment/nginx-deployment nginx=nginx:1.27

# Check rollout
kubectl rollout status deployment/nginx-deployment

# Rollback
kubectl rollout undo deployment/nginx-deployment

# Delete deployment
kubectl delete deployment nginx-deployment
```

## Key Takeaways

* **Deployment** is the standard way to run stateless applications in Kubernetes.
* A **Deployment manages ReplicaSets**, and **ReplicaSets manage Pods**.
* The **Pod template** defines what every Pod should look like.
* The **selector** and **Pod labels** must match so the Deployment knows which Pods it owns.
* Features like **self-healing**, **scaling**, **rolling updates**, and **rollbacks** are the main reasons Deployments are preferred over creating Pods directly.
* In production, you almost always create **Deployments**, not standalone Pods.
