---
name: kubernetes-deployment
description: '**WORKFLOW SKILL** — Deploy Spring Boot microservices to Kubernetes. USE FOR: creating manifests, ConfigMaps, Secrets, RBAC, health checks, port-forwarding. DO NOT USE FOR: Kubernetes cluster setup or networking policy management.'
---

# Kubernetes Deployment Workflow

## Overview
This skill guides the deployment of Spring Boot microservices to a Kubernetes cluster (minikube or production). It covers creating manifests, setting up configuration, secrets, RBAC, and validating deployments.

## Workflow Steps

1. **Prepare Cluster**
   - Start minikube: `minikube start --driver=docker`
   - Point Docker to minikube: `eval $(minikube docker-env)`
   - Verify cluster access: `kubectl cluster-info`

2. **Create Namespaces**
   - Create dedicated namespace for each service
   - Isolates resources and enables multi-team setups
   ```bash
   kubectl create ns spring-boot-service
   kubectl create ns spring-data-service
   kubectl create ns gateway-service
   ```

3. **Build Docker Images**
   - Build against minikube's Docker daemon (not your local Docker)
   - Tag images for the target registry
   ```bash
   docker build -t <user>/spring-boot-service:1.0.0-SNAPSHOT ./microservice-spring-boot
   docker build -t <user>/spring-data-service:1.0.0-SNAPSHOT ./microservice-spring-data
   docker build -t <user>/gateway-service:1.0.0-SNAPSHOT ./gateway-service
   ```

4. **Create Secrets**
   - Database credentials as Kubernetes Secrets
   - One secret per namespace
   ```bash
   kubectl -n spring-boot-service create secret generic db-secret \
     --from-literal=username=$DB_USER --from-literal=password=$DB_PASSWORD
   kubectl -n spring-data-service create secret generic db-secret \
     --from-literal=username=$DB_USER --from-literal=password=$DB_PASSWORD
   ```
   - For Astra: Create `astracreds` secret with secure-connect-bundle

5. **Create ConfigMaps**
   - Application config: contact points, keyspace, profiles
   - Use YAML files in `deploy/` folder
   ```bash
   kubectl -n spring-boot-service apply -f deploy/spring-boot/spring-boot-service-configmap.yml
   kubectl -n spring-data-service apply -f deploy/spring-data/spring-data-service-configmap.yml
   ```

6. **Create RBAC Rules**
   - ServiceAccount, Role, and RoleBinding for each service
   - Required for Spring Cloud Kubernetes config access
   ```bash
   kubectl -n spring-boot-service apply -f deploy/spring-boot/spring-boot-rbac.yml
   kubectl -n spring-data-service apply -f deploy/spring-data/spring-data-rbac.yml
   ```

7. **Deploy Services**
   - Apply Deployment and Service manifests
   ```bash
   kubectl -n spring-boot-service apply -f deploy/spring-boot/spring-boot-deployment.yml
   kubectl -n spring-boot-service apply -f deploy/spring-boot/spring-boot-service.yml
   kubectl -n spring-data-service apply -f deploy/spring-data/spring-data-deployment.yml
   kubectl -n spring-data-service apply -f deploy/spring-data/spring-data-service.yml
   ```

8. **Deploy Gateway**
   - Configure routes to point to internal service DNS
   - Example: `http://spring-boot-service.spring-boot-service.svc.cluster.local:80`
   ```bash
   kubectl -n gateway-service apply -f deploy/gateway/gateway-configmap.yml
   kubectl -n gateway-service apply -f deploy/gateway/gateway-deployment.yml
   kubectl -n gateway-service apply -f deploy/gateway/gateway-service.yml
   ```

9. **Verify Deployments**
   - Check pod status
   ```bash
   kubectl -n spring-boot-service get pods
   kubectl -n spring-data-service get pods
   kubectl -n gateway-service get pods
   ```
   - Check logs
   ```bash
   kubectl -n spring-boot-service logs <pod-name>
   kubectl logs -f deployment/spring-boot-service -n spring-boot-service
   ```

10. **Port Forward for Local Testing**
    - Forward gateway to localhost:8080
    ```bash
    GATEWAY_POD=$(kubectl -n gateway-service get pods | tail -n 1 | cut -f 1 -d ' ')
    kubectl -n gateway-service port-forward $GATEWAY_POD 8080:8080
    ```
    - Forward individual services for direct testing
    ```bash
    BOOT_SERVICE_POD=$(kubectl -n spring-boot-service get pods | tail -n 1 | cut -f 1 -d ' ')
    kubectl -n spring-boot-service port-forward $BOOT_SERVICE_POD 8083:8083
    ```

11. **Validate Connectivity**
    - Test through gateway
    ```bash
    curl http://localhost:8080/api/products/search/mobile
    curl http://localhost:8080/api/orders/search/order-by-id?orderId=...
    ```
    - Check internal DNS resolution
    ```bash
    kubectl -n spring-boot-service run -it --rm dns-test --image=busybox --restart=Never -- \
      nslookup spring-data-service.spring-data-service.svc.cluster.local
    ```

12. **Monitor & Troubleshoot**
    - Live pod metrics: `kubectl top pods -n spring-boot-service`
    - Describe pod for events: `kubectl describe pod <pod-name> -n spring-boot-service`
    - Stream logs: `kubectl logs -f pod/<pod-name> -n spring-boot-service`

## Deployment Manifest Template

**Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-service
  namespace: spring-boot-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-boot-service
  template:
    metadata:
      labels:
        app: spring-boot-service
    spec:
      containers:
      - name: spring-boot-service
        image: <user>/spring-boot-service:1.0.0-SNAPSHOT
        ports:
        - containerPort: 8083
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: kubernetes
        envFrom:
        - configMapRef:
            name: spring-boot-service-config
        - secretRef:
            name: db-secret
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 120
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 120
          periodSeconds: 10
```

**Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-service
  namespace: spring-boot-service
spec:
  selector:
    app: spring-boot-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8083
  type: ClusterIP
```

## Important Considerations

- **Image Pull Policy**: Image names in manifests must match your built images
- **Namespaces**: Keep services isolated; avoid default namespace
- **DNS**: Use full DNS within cluster: `{service}.{namespace}.svc.cluster.local`
- **Health Checks**: Configure liveness/readiness probes on management port (8084, 8082, 8085)
- **Initial Delays**: Account for Cassandra startup (120s delay recommended)
- **Astra Support**: Uncomment astravol volumes when using Astra

## Troubleshooting

| Issue | Solution |
|-------|----------|
| ImagePullBackOff | Build image against minikube: `eval $(minikube docker-env)` |
| CrashLoopBackOff | Check logs: `kubectl logs <pod> -n <ns>` |
| Connection refused | Verify ConfigMap with contact points, verify Cassandra is running |
| Pod pending | Check node resources: `kubectl top nodes`, `kubectl describe node` |
| DNS resolution fails | Verify service DNS: use full FQDN `service.namespace.svc.cluster.local` |

## Related Skills
- Use `cassandra-schema-design` to validate schemas in deployed clusters
- Use `spring-boot-code-review` to check manifest YAML for best practices