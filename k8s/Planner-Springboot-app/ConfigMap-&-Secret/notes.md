
## 2. ConfigMap

### 2.1 Why It Exists

Hardcoding config (like `DB_HOST=localhost`, feature flags, `.properties` files, `nginx.conf`) directly into your Docker image is bad because:
- You'd need to **rebuild the image** every time config changes.
- Same image can't be reused across dev/staging/prod environments.

**ConfigMap = a Kubernetes object to store non-sensitive configuration data (key-value pairs or whole files) separately from your application image**, and inject it into Pods at runtime.

### 2.2 How It's Used (3 common ways)

1. As **environment variables** inside a container
2. As **command-line arguments**
3. As **mounted files/volumes** inside the container (great for full config files like `nginx.conf`)

### 2.3 Diagram

```
 ┌────────────────────┐
 │     ConfigMap       │   key: value data (non-secret)
 │  APP_MODE=prod      │
 │  LOG_LEVEL=info      │
 └─────────┬───────────┘
           │ injected as
     ┌─────┴──────┬───────────────┐
     ▼             ▼               ▼
  Env Var      CLI Args        Mounted Volume
     │             │               │
     └─────────────┴───────────────┘
                    ▼
                  Pod / Container
```

### 2.4 YAML Example — ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  APP_MODE: "production"       # simple key-value pair
  LOG_LEVEL: "info"
  config.json: |                # can also hold entire file contents as a value
    {
      "retries": 3,
      "timeout": "30s"
    }
```

### 2.5 Using ConfigMap as Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  containers:
    - name: app
      image: nginx
      envFrom:
        - configMapRef:
            name: app-config     # injects ALL key-values as env vars automatically
      # OR inject a single specific key:
      env:
        - name: MODE
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: APP_MODE
```

### 2.6 Using ConfigMap as a Mounted Volume

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  containers:
    - name: app
      image: nginx
      volumeMounts:
        - name: config-vol
          mountPath: /etc/config    # config.json will appear as a file here
  volumes:
    - name: config-vol
      configMap:
        name: app-config
```

Every key in `data` becomes a **file** inside `/etc/config` (e.g. `/etc/config/config.json`), and the value becomes the file's content. This is the standard way to inject files like `nginx.conf`, `application.yaml`, etc.
---

## 3. Secret

### 3.1 Why It Exists

Secret is **structurally identical to ConfigMap**, but meant for **sensitive data**: passwords, API keys, TLS certs, tokens. Kubernetes treats Secrets a bit differently:

- Values are stored **base64-encoded** (NOT encrypted by default — base64 is just encoding, easily reversible, so it's not true security by itself).
- Can be encrypted at rest if you enable **encryption at rest** on the cluster (etcd encryption) — production clusters should do this.
- Kubernetes avoids printing Secret values in `kubectl describe`, logs, etc. by default.
- Often integrated with external secret managers (AWS Secrets Manager, HashiCorp Vault, Sealed Secrets) in real production setups instead of raw K8s Secrets.

### 3.2 YAML Example — Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque                     # Opaque = generic key-value secret (most common type)
data:
  DB_USER: YWRtaW4=              # base64 encoded value of "admin"
  DB_PASS: cGFzc3dvcmQxMjM=      # base64 encoded value of "password123"
```

To generate the base64 values yourself:
```bash
echo -n 'admin' | base64        # -> YWRtaW4=
echo -n 'password123' | base64  # -> cGFzc3dvcmQxMjM=
```

Alternatively, use `stringData` to write plain text and let K8s encode it for you:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
stringData:
  DB_USER: admin              # plain text — K8s encodes automatically on creation
  DB_PASS: password123
```

### 3.3 When Does the Base64 Conversion Actually Happen?

This is a common point of confusion, so worth being precise about it:

**The conversion happens at object-creation time on the API server — NOT inside the Pod, and NOT "after running the pod."**

Here's the exact sequence when you do `kubectl apply -f secret.yaml` with `stringData`:

```
You write:                 kube-apiserver receives it:            Stored in etcd:
stringData:                 1. reads stringData (plain text)       data:
  DB_USER: admin      ──▶   2. base64-encodes each value    ──▶      DB_USER: YWRtaW4=
  DB_PASS: password123      3. merges it into `data`                 DB_PASS: cGFzc3dvcmQxMjM=
                             4. discards stringData field
                                (it's write-only, never stored)
```

So:
- The moment you `kubectl apply`/`kubectl create` the Secret, the API server converts `stringData` → base64 and writes it into the `data` field in etcd. This happens **once, at creation/update time**, regardless of whether any Pod is using it yet.
- If you then run `kubectl get secret db-secret -o yaml`, you will only ever see the base64-encoded `data` field — `stringData` is a convenience input field, it's never actually persisted as-is.
- **When the Pod starts and mounts/reads the Secret**, the kubelet does the *reverse*: it base64-**decodes** the value before injecting it as an env var or writing it as a file inside the container. So your application (e.g. Spring Boot reading `DB_PASS`) sees the original plain text `password123`, not the base64 string — decoding is transparent to your app.
- Nothing changes "after running the pod" — the pod doesn't trigger any encoding. Encoding happens when the Secret object is created; decoding happens when the kubelet injects it into a running container.

**tl;dr:** `stringData` in → base64 `data` out, done by the API server at creation time → decoded back to plain text automatically by kubelet when a Pod consumes it. You (and your app) never manually handle base64 either way.

### 3.4 Using Secret in a Pod (same pattern as ConfigMap)

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-app
spec:
  containers:
    - name: app
      image: nginx
      env:
        - name: DATABASE_USER
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: DB_USER
        - name: DATABASE_PASS
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: DB_PASS
      # OR mount as files:
      volumeMounts:
        - name: secret-vol
          mountPath: /etc/secret
          readOnly: true
  volumes:
    - name: secret-vol
      secret:
        secretName: db-secret
```

### 3.5 ConfigMap vs Secret — Quick Comparison

| | ConfigMap | Secret |
|---|---|---|
| Purpose | Non-sensitive config | Sensitive data (passwords, keys, certs) |
| Encoding | Plain text | Base64 (not true encryption alone) |
| Storage in etcd | Plain | Plain unless etcd encryption at rest is enabled |
| Visible via `kubectl get -o yaml` | Yes, in plain | Yes, but base64 encoded |
| Usage pattern | env vars / volume / CLI args | env vars / volume (same pattern) |
| Real prod best practice | Fine as-is | Often paired with Vault / Sealed Secrets / cloud secret manager |

---

## 4. Mental Model to Tie It All Together

```
StorageClass → automates disk creation (infrastructure-level: "where does my data live")
ConfigMap    → injects non-sensitive settings into your app (app-level config)
Secret       → injects sensitive settings into your app (app-level, guarded)
```

All three follow the same underlying K8s pattern:
**Define an object → reference it from the Pod spec (env / volume) → Kubernetes wires it up at runtime, decoupled from your image.**