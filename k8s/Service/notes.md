# Kubernetes Service - Summary Notes

# What is a Service?

A **Service** is a Kubernetes object that provides a **stable network endpoint** for a set of Pods.

A Service allows clients to communicate with Pods **without knowing their IP addresses**.

---

# Why do we need a Service?

Suppose you have two Pods.

```text
Pod A
IP = 10.1.0.5

Pod B
IP = 10.1.0.6
```

Everything works.

Now Pod A crashes.

```text
Pod A ❌
```

Deployment creates another Pod.

```text
Pod C
IP = 10.1.0.20
```

Notice:

```text
Old IP
10.1.0.5

↓

New IP
10.1.0.20
```

The Pod IP changed.

If another application was talking directly to

```text
10.1.0.5
```

it now fails.

Pods are **ephemeral**.

A Service solves this problem.

---

# Service Architecture

Without Service

```text
Client
   │
   ▼
Pod A (10.1.0.5)

Problem:
If Pod dies,
IP changes.
```

With Service

```text
Client
   │
   ▼
Service
10.96.15.30
   │
   ├────────► Pod A
   ├────────► Pod B
   └────────► Pod C
```

The Service IP remains stable.

Pods can come and go.

---

# Responsibilities of a Service

A Service provides:

* Stable IP
* Stable DNS name
* Load Balancing
* Service Discovery
* Access to Pods

---

# How does a Service know which Pods belong to it?

Using **labels**.

Pods

```yaml
labels:
    app: nginx
```

Service

```yaml
selector:
    app: nginx
```

The Service automatically finds every Pod with:

```text
app=nginx
```

---

# Service YAML

```yaml
apiVersion: v1
kind: Service

metadata:
  name: nginx-service

spec:

  selector:
    app: nginx

  ports:
    - port: 80
      targetPort: 80

  type: ClusterIP
```

---

# Important Sections

## selector

```yaml
selector:
    app: nginx
```

This tells Kubernetes

> Forward requests to every Pod having

```text
app=nginx
```

---

## port

```yaml
port: 80
```

The Service listens on

```text
Port 80
```

---

## targetPort

```yaml
targetPort: 80
```

The request is forwarded to

```text
Container Port 80
```

Example

```text
Browser

↓

Service Port 8080

↓

Container Port 80
```

YAML

```yaml
ports:
    - port: 8080
      targetPort: 80
```

---

# Verify Service

```bash
kubectl get svc
```

Example

```text
NAME             TYPE         CLUSTER-IP      PORT(S)
nginx-service    ClusterIP    10.96.1.20      80/TCP
```

---

# Describe Service

```bash
kubectl describe service nginx-service
```

Shows

* Labels
* Selector
* Port
* Endpoints

---

# Check Service Endpoints

Very important command

```bash
kubectl get endpoints nginx-service
```

Example

```text
NAME             ENDPOINTS
nginx-service    10.1.0.5:80,10.1.0.6:80
```

Meaning

The Service is forwarding requests to both Pods.

---

# Kubernetes Service Types

There are **four** main Service types.

```text
ClusterIP

NodePort

LoadBalancer

ExternalName
```

Let's understand each.

---

# 1. ClusterIP (Default)

Most commonly used.

```yaml
type: ClusterIP
```

Architecture

```text
Pod A

Pod B

      ▲
      │
ClusterIP Service

      ▲
      │
Another Pod
```

Accessible only **inside** the cluster.

Cannot be accessed from your laptop.

---

### Use Cases

* Microservices communication
* Backend APIs
* Database access
* Internal applications

Example

```text
Order Service

↓

Payment Service

↓

Inventory Service
```

All communicate using ClusterIP Services.

---

# 2. NodePort

Exposes the Service on every Kubernetes node.

```yaml
type: NodePort
```

Architecture

```text
Browser

↓

NodeIP:30080

↓

NodePort Service

↓

Pods
```

YAML

```yaml
ports:
    - port: 80
      targetPort: 80
      nodePort: 30080

type: NodePort
```

Access

```text
http://NodeIP:30080
```

NodePort range

```text
30000-32767
```

---

### Use Cases

* Local development
* Testing
* Minikube
* Docker Desktop

Rarely used directly in production.

---

# 3. LoadBalancer

Most popular in cloud.

```yaml
type: LoadBalancer
```

Architecture

```text
Internet
     │
     ▼
Cloud Load Balancer
     │
     ▼
Service
     │
     ▼
Pods
```

On AWS

```text
AWS ELB

↓

Service

↓

Pods
```

On Azure

```text
Azure Load Balancer

↓

Pods
```

On GCP

```text
Google Cloud Load Balancer
```

---

### Docker Desktop

Docker Desktop maps the LoadBalancer to:

```text
localhost
```

That's why:

```text
http://localhost:8082
```

worked for you.

On cloud,

you'll receive a real external IP.

---

### Use Cases

* Public APIs
* Frontend applications
* Public websites

---

# 4. ExternalName

No proxying.

No Pods.

No selectors.

Simply maps one DNS name to another.

Example

```yaml
apiVersion: v1
kind: Service

metadata:
    name: google

spec:
    type: ExternalName
    externalName: google.com
```

Inside Kubernetes

```text
google.default.svc.cluster.local
```

automatically resolves to

```text
google.com
```

---

### Use Cases

* External databases
* External APIs
* Third-party services

Example

```text
RDS

Stripe

Redis Cloud

Mongo Atlas
```

---

# Service Types Comparison

| Service Type | Accessible From                                | Use Case                   |
| ------------ | ---------------------------------------------- | -------------------------- |
| ClusterIP    | Inside cluster only                            | Microservice communication |
| NodePort     | Node IP + Port                                 | Local testing, demos       |
| LoadBalancer | Internet (cloud) or localhost (Docker Desktop) | Public applications        |
| ExternalName | DNS alias                                      | External services          |

---

# Service Discovery

Every Service gets a DNS name.

Suppose

```text
Service

payment-service
```

Inside cluster

Other Pods can call

```text
http://payment-service
```

No IP required.

Kubernetes DNS resolves it.

---

# Load Balancing

Suppose

```text
Pod1

Pod2

Pod3
```

Client sends

```text
10 Requests
```

Kubernetes distributes them.

```text
Request1 → Pod1

Request2 → Pod2

Request3 → Pod3

Request4 → Pod1

...
```

You don't choose the Pod.

The Service does.

---

# Delete Service

```bash
kubectl delete service nginx-service
```

or

```bash
kubectl delete -f service.yaml
```

---

# Most Common Commands

```bash
# Create Service
kubectl apply -f service.yaml

# View Services
kubectl get svc

# View Endpoints
kubectl get endpoints

# Describe Service
kubectl describe service nginx-service

# Port Forward
kubectl port-forward service/nginx-service 8080:80

# Delete Service
kubectl delete service nginx-service
```

---

# Interview Questions

## Why do we need a Service?

Pods are ephemeral and their IP addresses change when they're recreated. A Service provides a stable IP address and DNS name so applications can communicate reliably.

---

## Does a Service create Pods?

**No.**

Deployments (through ReplicaSets) create Pods.

A Service only routes traffic to existing Pods.

---

## How does a Service identify Pods?

Using **label selectors**.

Service

```yaml
selector:
    app: nginx
```

Pod

```yaml
labels:
    app: nginx
```

The labels must match.

---

## Can one Service route traffic to multiple Pods?

**Yes.**

That's one of its main responsibilities.

---

## Can one Pod belong to multiple Services?

**Yes.**

Example:

```text
                 Frontend Service
                        │
                        ▼
                   Web Pod
                        ▲
                        │
                 Metrics Service
```

As long as the Pod's labels match the selectors of both Services, it can receive traffic from both.

---

## Difference between port, targetPort, and nodePort

| Field        | Meaning                                                                                  |
| ------------ | ---------------------------------------------------------------------------------------- |
| `port`       | The port exposed by the Service.                                                         |
| `targetPort` | The port on the Pod/container where traffic is forwarded.                                |
| `nodePort`   | The port exposed on each Kubernetes node (used only with `NodePort` and `LoadBalancer`). |

Example:

```yaml
ports:
  - port: 80
    targetPort: 8080
    nodePort: 30080
```

Request flow:

```text
Client
    │
    ▼
NodeIP:30080
    │
    ▼
Service Port 80
    │
    ▼
Container Port 8080
```

---

# Service vs Deployment

| Deployment               | Service                       |
| ------------------------ | ----------------------------- |
| Creates and manages Pods | Exposes Pods over the network |
| Handles scaling          | Handles traffic routing       |
| Self-healing             | Stable IP and DNS             |
| Rolling updates          | Load balancing                |
| Rollbacks                | Service discovery             |

---

# Complete Request Flow

A typical request in Kubernetes follows this path:

```text
Internet / Browser
        │
        ▼
LoadBalancer (or NodePort)
        │
        ▼
Service
        │
        ▼
Deployment
        │
        ▼
ReplicaSet
        │
        ▼
Pod
        │
        ▼
Container
```

## Key Takeaways

* **Services don't run applications**; they provide a stable way to access Pods.
* A Service uses **selectors** to find Pods based on their **labels**.
* **ClusterIP** is for internal communication.
* **NodePort** exposes an application on every node using a port between **30000–32767**.
* **LoadBalancer** exposes an application externally using a cloud load balancer (or `localhost` on Docker Desktop).
* **ExternalName** maps a Kubernetes Service name to an external DNS name without creating any proxy or endpoint.
* In a production application, you'll commonly see the combination: **Deployment + Service**, where the Deployment manages the Pods and the Service provides stable access to them.
