# Kubernetes Volumes: emptyDir, hostPath, PV & PVC

Volumes in Kubernetes work differently from Docker, so let's build this up step by step, starting with the concept and ending with your actual MySQL deployment fixed up.

## The core idea

In Kubernetes, a volume is defined in **two parts**:

1. **`volumes:`** (at the Pod/spec level) — *where the storage actually comes from*
2. **`volumeMounts:`** (inside each container) — *where that storage appears inside the container's filesystem*

You always need both. Think of `volumes` as "here's a disk I have," and `volumeMounts` as "plug that disk into this folder inside my container."

```yaml
spec:
  containers:
    - name: mysql-pod-container
      volumeMounts:
        - name: mysql-storage        # must match the volume name below
          mountPath: /var/lib/mysql  # where MySQL stores its data
  volumes:
    - name: mysql-storage
      <type of volume goes here>
```

Now let's go through each type.

---

## 1. `emptyDir`

**What it is:** A temporary, empty folder created *when the pod starts*, and **deleted when the pod dies**. It's shared between containers in the same pod, but not persistent across restarts.

**When to use it:** Scratch space, caching, temp files — never for a database you care about.

```yaml
volumes:
  - name: temp-storage
    emptyDir: {}
```

❌ **Don't use this for MySQL** — if the pod restarts (which happens often — crashes, updates, node moves), your entire database disappears.

---

## 2. `hostPath`

**What it is:** Mounts a folder from the **worker node's actual filesystem** into the pod. E.g., `/data/mysql` on the physical/VM node.

```yaml
volumes:
  - name: mysql-storage
    hostPath:
      path: /data/mysql
      type: DirectoryOrCreate
```

**When to use it:** Local testing on a single-node cluster (like Minikube or Docker Desktop's Kubernetes).

**Why it's risky in real clusters:** If your pod gets rescheduled to a *different* node (which Kubernetes does automatically), the data isn't there anymore — it's tied to one specific machine. Also multiple pods writing to the same host path can conflict. It's basically Kubernetes' version of a Docker bind mount, with the same node-locality problem magnified by clustering.

Good for learning/local dev, bad for anything resembling production.

---

## 3. PV (PersistentVolume) and PVC (PersistentVolumeClaim)

This is the real, production-grade way to do storage in Kubernetes — and it's the one you should use for MySQL.

**The mental model:**
- **PV (PersistentVolume)** = the actual storage resource that exists in your cluster (could be a cloud disk like AWS EBS/GCP PD, an NFS share, or even a hostPath under the hood). Think of it as "a disk that exists, administered separately from your app."
- **PVC (PersistentVolumeClaim)** = a *request* for storage made by your pod. Think of it as "I need 1Gi of storage, please give me a matching disk."

Kubernetes automatically **binds** a PVC to a matching PV. Your pod then references the **PVC** (not the PV directly).

```
Pod → PVC ("I need storage") → PV ("here's actual storage") → Real disk
```

This separation is powerful because:
- The pod doesn't care *where* the storage physically lives.
- The PVC survives even if the pod is deleted and recreated — so your data survives restarts, crashes, and rescheduling.
- Admins/cloud providers can manage the actual disks separately from app developers.

### Static provisioning (you manually define the PV)

**PersistentVolume:**
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: mysql-pv
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteOnce                    # only one node can mount it read-write at a time
  persistentVolumeReclaimPolicy: Retain # what happens to the PV after its PVC is deleted
  hostPath:                             # for local testing; cloud clusters use different backends
    path: /data/mysql
```

**PersistentVolumeClaim:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

Kubernetes sees these two, notices they're compatible (access mode matches, and the PV's capacity is *at least* as big as what the PVC asks for), and **binds** them automatically.

A couple of things worth understanding about the fields above:

**What if the PV is bigger than the PVC's request?** In the example above, the PV has `2Gi` but the PVC only asks for `1Gi`. They still bind — a PV just needs to be *large enough*, not an exact match. But binding is **1:1 and whole**: the PVC ends up claiming the **entire 2Gi**, not just the 1Gi it requested, and your pod actually gets 2Gi of usable storage. The "extra" capacity isn't shared or split off to any other PVC — that PV is now fully consumed by this one claim. If several PVs of different sizes exist, Kubernetes' default binder picks the **smallest PV that still satisfies the request** (best fit, not first fit). This is one reason dynamic provisioning (below) is often preferred — it creates a PV of the *exact* size requested, with no leftover waste.

**What `persistentVolumeReclaimPolicy` does:** this controls what happens to the PV (and its data) once the PVC using it is deleted. There are three options:
- **`Retain`** (used above) — the PV and its data are kept. It moves to a `Released` state and is **not** automatically reused — an admin has to manually clean it up and re-bind or delete it before it can serve a new PVC. Safest option, since nothing gets wiped without a human deciding to. Best for databases like MySQL.
- **`Delete`** — when the PVC is deleted, the PV **and the underlying storage** (e.g. the actual cloud disk) are deleted automatically too. This is the **default for dynamically provisioned volumes**. Fine for temporary/dev storage, risky for production data without backups.
- **`Recycle`** *(deprecated, avoid)* — old behavior that scrubbed the volume's data and made it available again for a new PVC. No longer recommended; use dynamic provisioning instead.

For your MySQL setup, `Retain` is the right call — it protects your database files from being wiped out if the PVC or deployment ever gets deleted accidentally.

### Dynamic provisioning (the more common real-world approach)

In real clusters (EKS, GKE, AKS, etc.), you usually **skip creating the PV yourself**. Instead, you just create a PVC, and a `StorageClass` in the background automatically provisions a matching PV (e.g., spins up an actual cloud disk) for you.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  storageClassName: standard   # cluster-provided, e.g. gp2, standard, etc.
```

No PV needed from you — it gets created automatically. This is what you'll do on any real cloud cluster. Locally (Minikube/Docker Desktop), a default StorageClass usually already exists too, so even locally you often just need the PVC.

---

## Applying this to your MySQL deployment

Here's your setup, fixed with PV + PVC (static provisioning shown here since it's clearer for learning; switch to dynamic by dropping the PV and adding `storageClassName` once you're on a real cluster):

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: mysql-pv
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: /data/mysql
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-deployment
  labels:
    app: mysql-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql-label-app-name
  template:
    metadata:
      name: mysql-pod
      labels:
        app: mysql-label-app-name
    spec:
      containers:
      - name: mysql-pod-container
        image: mysql:8.4
        ports:
          - containerPort: 3306
        env:
          - name: MYSQL_ROOT_PASSWORD
            value: root
          - name: MYSQL_DATABASE
            value: planner_db
          - name: MYSQL_USER
            value: username
          - name: MYSQL_PASSWORD
            value: pwd
        volumeMounts:
          - name: mysql-storage
            mountPath: /var/lib/mysql   # <-- this is MySQL's actual data directory
        resources:
          requests:
            memory: "250Mi"
            cpu: 512m
          limits:
            memory: "1Gi"
            cpu: "1"
      volumes:
        - name: mysql-storage
          persistentVolumeClaim:
            claimName: mysql-pvc
```

The key line is `mountPath: /var/lib/mysql` — that's where the MySQL image internally stores its actual database files, so that's what needs to sit on persistent storage.

### One important gotcha with `replicas: 1` + PVC

Since your access mode is `ReadWriteOnce`, only one pod can mount that volume at a time — which is fine for `replicas: 1`, but if you ever scale MySQL to multiple replicas, this setup will break (they'd fight over the same volume, and Kubernetes won't even schedule the second pod). Real multi-instance MySQL setups use StatefulSets with `volumeClaimTemplates` (a separate PVC per replica) instead — but that's a step beyond what you need right now.

---

## Quick summary / decision guide

| Type | Survives pod restart? | Survives node change? | Use case |
|---|---|---|---|
| `emptyDir` | ❌ No | ❌ No | Temp/scratch data only |
| `hostPath` | ✅ Yes | ❌ No | Single-node local dev |
| PV + PVC | ✅ Yes | ✅ Yes | Real persistent data (databases!) |

For your MySQL deployment specifically, **PV + PVC is the right choice** — it's the Kubernetes equivalent of the Docker named volume you were likely using in `docker-compose`.