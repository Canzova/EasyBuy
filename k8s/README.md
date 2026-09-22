# ☸️ Kubernetes Hands-on Learning Repository

Welcome to your complete hands-on Kubernetes workspace!

For the structured, end-to-end study order mapping all folders, `notes.md` files, YAML manifests, architecture diagrams, and the master PDF companion, please see:

👉 **[Master Kubernetes Learning Path & Roadmap (LEARNING_PATH.md)](./LEARNING_PATH.md)**

---

### Quick Directory Reference

- **Architecture Overview**: [`Basics.png`](./Basics.png) & [`k8s_complete_guide_lyst1785594969577.pdf`](./k8s_complete_guide_lyst1785594969577.pdf)
- **Phase 1: Core Primitives**:
  - [`Pod/`](./Pod/) — [`notes.md`](./Pod/notes.md), [`nginx-pod.yaml`](./Pod/nginx-pod.yaml)
  - [`Deployment/`](./Deployment/) — [`notes.md`](./Deployment/notes.md), [`nginx-deployment.yaml`](./Deployment/nginx-deployment.yaml)
  - [`Service/`](./Service/) — [`notes.md`](./Service/notes.md), [`nginx-service.yaml`](./Service/nginx-service.yaml)
- **Phase 2: Configuration & Volumes**:
  - [`Planner-Springboot-app/ConfigMap-&-Secret/`](./Planner-Springboot-app/ConfigMap-&-Secret/) — [`notes.md`](./Planner-Springboot-app/ConfigMap-&-Secret/notes.md)
  - [`Planner-Springboot-app/Volume/`](./Planner-Springboot-app/Volume/) — [`notes.md`](./Planner-Springboot-app/Volume/notes.md), [`volume-locations-diagram.svg`](./Planner-Springboot-app/Volume/volume-locations-diagram.svg), [`access-modes-diagram.svg`](./Planner-Springboot-app/Volume/access-modes-diagram.svg)
- **Phase 3: Real Application**:
  - [`Planner-Springboot-app/`](./Planner-Springboot-app/) — Spring Boot API + MySQL Deployment
  - [`Storage Class/`](./Storage%20Class/) — [`notes`](./Storage%20Class/notes), [`storageclass-diagram.png`](./Storage%20Class/storageclass-diagram.png)
- **Phase 4: Production Stateful Architecture & Ingress**:
  - [`Stateful-Planner-App/StatefulSet/`](./Stateful-Planner-App/StatefulSet/) — [`notes.md`](./Stateful-Planner-App/StatefulSet/notes.md), [`mysql-stateful.yaml`](./Stateful-Planner-App/StatefulSet/mysql-stateful.yaml)
  - [`Stateful-Planner-App/Ingress/`](./Stateful-Planner-App/Ingress/) — [`notes.md`](./Stateful-Planner-App/Ingress/notes.md), [`ingress-routing-diagram.png`](./Stateful-Planner-App/Ingress/ingress-routing-diagram.png)
- **Phase 5: Autoscaling & Helm**:
  - [`Planner-Springboot-app/hpa/`](./Planner-Springboot-app/hpa/) — [`notes.md`](./Planner-Springboot-app/hpa/notes.md), [`planner-hpa.yaml`](./Planner-Springboot-app/hpa/planner-hpa.yaml)
  - [`planner-helm/`](./planner-helm/) — [`README.md`](./planner-helm/README.md), [`helm-chart-rendering-flow.svg`](./planner-helm/helm-chart-rendering-flow.svg)
