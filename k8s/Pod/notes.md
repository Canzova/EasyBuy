# 🚀 Kubernetes Basic Workflow Cheat Sheet

## Step 1: Check your Kubernetes cluster

Before doing anything, make sure your cluster is running.

```bash
kubectl cluster-info
```

Check the nodes:

```bash
kubectl get nodes
```

Expected:

```text
NAME                 STATUS   ROLES           AGE
docker-desktop       Ready    control-plane   ...
```

---

# Step 2: Create a Namespace

Create a namespace for your application.

```bash
kubectl create namespace learn-namespace
```

Verify:

```bash
kubectl get namespaces
```

Output:

```text
NAME
default
kube-system
learn-namespace
```

---

# Step 3: Make that namespace your default

Instead of typing `-n learn-namespace` every time:

```bash
kubectl config set-context --current --namespace=learn-namespace
```

Verify:

```bash
kubectl config view --minify | grep namespace
```

Output:

```text
namespace: learn-namespace
```

> **Remember:** Your context name (e.g. `docker-desktop`) does **not** change. Only its default namespace changes.

---

# Step 4: Verify the current context

```bash
kubectl config current-context
```

Example:

```text
docker-desktop
```

See all contexts:

```bash
kubectl config get-contexts
```

---

# Step 5: Write your YAML

Example:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
spec:
  containers:
  - name: nginx-pod-container
    image: nginx:latest
    ports:
      - containerPort: 80
    resources:
      requests:
        memory: "128Mi"
        cpu: 100m
      limits:
        memory: "265Mi"
        cpu: "500m"
```

---

# Step 6: Validate the YAML (optional but recommended)

Instead of immediately creating it:

```bash
kubectl apply --dry-run=client -f nginx-pod.yaml
```

If valid:

```text
pod/nginx-pod created (dry run)
```

---

# Step 7: Create the Pod

```bash
kubectl apply -f nginx-pod.yaml
```

Output:

```text
pod/nginx-pod created
```

---

# Step 8: Verify the Pod

Most common command:

```bash
kubectl get pods
```

Example:

```text
NAME        READY   STATUS    RESTARTS   AGE
nginx-pod   1/1     Running   0          12s
```

---

# Step 9: Get detailed information

Whenever something isn't working:

```bash
kubectl describe pod nginx-pod
```

This is your debugging command.

Check:

* Events
* Image Pull
* Scheduling
* Container State

---

# Step 10: View logs

```bash
kubectl logs nginx-pod
```

If multiple containers:

```bash
kubectl logs nginx-pod -c container-name
```

---

# Step 11: Execute commands inside the Pod

```bash
kubectl exec -it nginx-pod -- /bin/bash
```

or

```bash
kubectl exec -it nginx-pod -- /bin/sh
```

Useful for checking:

```bash
ls

pwd

env

nginx -v
```

Exit:

```bash
exit
```

---

# Step 12: Port Forward

Expose the Pod to your local machine.

```bash
kubectl port-forward pod/nginx-pod 8080:80
```

Meaning:

```
Your Laptop           Kubernetes Pod

localhost:8080 -----> Pod Port 80
```

Now open:

```
http://localhost:8080
```

or

```bash
curl http://localhost:8080
```

---

# Step 13: See Pod IP

```bash
kubectl get pod nginx-pod -o wide
```

Output:

```text
NAME        READY   STATUS    IP
nginx-pod   1/1     Running   10.1.0.12
```

---

# Step 14: See all resources

Pods:

```bash
kubectl get pods
```

Services:

```bash
kubectl get svc
```

Deployments:

```bash
kubectl get deployments
```

Everything:

```bash
kubectl get all
```

---

# Step 15: Delete the Pod

Using YAML:

```bash
kubectl delete -f nginx-pod.yaml
```

Using name:

```bash
kubectl delete pod nginx-pod
```

---

# Step 16: Delete the Namespace

```bash
kubectl delete namespace learn-namespace
```

Everything inside it will be deleted.

---

# Useful Debugging Commands

See everything:

```bash
kubectl get all
```

Watch Pods live:

```bash
kubectl get pods -w
```

Describe a namespace:

```bash
kubectl describe namespace learn-namespace
```

See Pod YAML:

```bash
kubectl get pod nginx-pod -o yaml
```

See Pod JSON:

```bash
kubectl get pod nginx-pod -o json
```

---

# Your Typical Development Workflow

```text
Start Cluster
      │
      ▼
kubectl cluster-info
      │
      ▼
kubectl get nodes
      │
      ▼
Create Namespace
      │
      ▼
kubectl create namespace learn-namespace
      │
      ▼
Set Default Namespace
      │
      ▼
kubectl config set-context --current --namespace=learn-namespace
      │
      ▼
Write YAML
      │
      ▼
kubectl apply --dry-run=client -f pod.yaml
      │
      ▼
kubectl apply -f pod.yaml
      │
      ▼
kubectl get pods
      │
      ▼
kubectl describe pod nginx-pod
      │
      ▼
kubectl logs nginx-pod
      │
      ▼
kubectl exec -it nginx-pod -- /bin/sh
      │
      ▼
kubectl port-forward pod/nginx-pod 8080:80
      │
      ▼
Open http://localhost:8080
      │
      ▼
kubectl delete -f pod.yaml
```

---

# ⭐ Commands You'll Use Every Day

| Task                  | Command                                                   |
| --------------------- | --------------------------------------------------------- |
| Check cluster         | `kubectl cluster-info`                                    |
| Check nodes           | `kubectl get nodes`                                       |
| Create namespace      | `kubectl create namespace <name>`                         |
| Set default namespace | `kubectl config set-context --current --namespace=<name>` |
| Apply YAML            | `kubectl apply -f file.yaml`                              |
| Validate YAML         | `kubectl apply --dry-run=client -f file.yaml`             |
| List Pods             | `kubectl get pods`                                        |
| List everything       | `kubectl get all`                                         |
| Describe a Pod        | `kubectl describe pod <pod-name>`                         |
| View logs             | `kubectl logs <pod-name>`                                 |
| Execute inside a Pod  | `kubectl exec -it <pod-name> -- /bin/sh`                  |
| Port forward          | `kubectl port-forward pod/<pod-name> 8080:80`             |
| Delete using YAML     | `kubectl delete -f file.yaml`                             |
| Delete by name        | `kubectl delete pod <pod-name>`                           |
| Watch resources       | `kubectl get pods -w`                                     |

---

As you continue learning, the next natural topics are **ReplicaSets**, **Deployments**, and **Services**. Those build directly on everything you've practiced here and are what you'll use in most real Kubernetes applications.
