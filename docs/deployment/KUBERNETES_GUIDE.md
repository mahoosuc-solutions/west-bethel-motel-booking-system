# Kubernetes Deployment Guide

## Overview

Complete guide for deploying and managing the West Bethel Motel Booking System on Kubernetes.

## Prerequisites

- kubectl 1.27+
- Kubernetes cluster (EKS, GKE, or self-managed)
- Helm 3.12+ (optional)
- Access to container registry

---

## Kubernetes Manifests

### Directory Structure

```
k8s/
├── namespace.yaml           # Namespace definition
├── configmap.yaml          # Application configuration
├── secret.yaml             # Sensitive data (template)
├── serviceaccount.yaml     # Service account for pods
├── deployment.yaml         # Application deployment
├── service.yaml            # Service definition
├── ingress.yaml            # Ingress configuration
├── hpa.yaml                # Horizontal Pod Autoscaler
├── pdb.yaml                # Pod Disruption Budget
└── networkpolicy.yaml      # Network policies
```

---

## Quick Deployment

```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Create secrets (customize first!)
kubectl create secret generic app-secrets \
  --from-literal=db-password=YOUR_DB_PASSWORD \
  --from-literal=jwt-secret=YOUR_JWT_SECRET \
  --from-literal=redis-password=YOUR_REDIS_PASSWORD \
  -n production

# 3. Apply all manifests
kubectl apply -f k8s/

# 4. Check deployment status
kubectl rollout status deployment/motel-booking-app -n production

# 5. Verify pods are running
kubectl get pods -n production
```

---

## Detailed Configuration

### 1. Namespace

**k8s/namespace.yaml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    name: production
    app: motel-booking-system
```

### 2. ConfigMap

**k8s/configmap.yaml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: motel-booking-config
  namespace: production
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SERVER_PORT: "8080"
  MANAGEMENT_PORT: "8081"
  LOGGING_LEVEL_ROOT: "INFO"
  # Add non-sensitive configuration here
```

### 3. Secrets (Sealed Secrets Recommended)

**Create secrets:**
```bash
# Database credentials
kubectl create secret generic db-credentials \
  --from-literal=url=jdbc:postgresql://db-host:5432/motel_booking \
  --from-literal=username=moteluser \
  --from-literal=password=SECURE_PASSWORD \
  -n production

# JWT secret
kubectl create secret generic jwt-secret \
  --from-literal=secret=$(openssl rand -base64 64) \
  -n production

# SMTP credentials
kubectl create secret generic smtp-credentials \
  --from-literal=host=smtp.gmail.com \
  --from-literal=username=noreply@westbethelmotel.com \
  --from-literal=password=APP_PASSWORD \
  -n production
```

### 4. Deployment

**Key features:**
- Rolling update strategy
- Resource limits
- Health checks
- Security context (non-root)
- Anti-affinity rules
- Lifecycle hooks

**k8s/deployment.yaml excerpt:**
```yaml
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
      containers:
      - name: motel-booking-app
        image: ghcr.io/westbethel/motel-booking-system:latest
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 120
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 5
```

### 5. Service

**k8s/service.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: motel-booking-service
  namespace: production
spec:
  type: ClusterIP
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: management
    port: 8081
    targetPort: 8081
  selector:
    app: motel-booking-app
```

### 6. Ingress

**k8s/ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: motel-booking-ingress
  namespace: production
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - www.westbethelmotel.com
    secretName: motel-booking-tls
  rules:
  - host: www.westbethelmotel.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: motel-booking-service
            port:
              number: 80
```

### 7. Horizontal Pod Autoscaler

**k8s/hpa.yaml:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: motel-booking-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: motel-booking-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## Operations

### Scaling

```bash
# Manual scaling
kubectl scale deployment/motel-booking-app --replicas=5 -n production

# Auto-scaling is configured via HPA
kubectl get hpa -n production
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/motel-booking-app \
  motel-booking-app=ghcr.io/westbethel/motel-booking-system:v1.1.0 \
  -n production

# Check rollout status
kubectl rollout status deployment/motel-booking-app -n production

# View rollout history
kubectl rollout history deployment/motel-booking-app -n production
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/motel-booking-app -n production

# Rollback to specific revision
kubectl rollout undo deployment/motel-booking-app \
  --to-revision=3 \
  -n production
```

### Monitoring

```bash
# Pod status
kubectl get pods -n production -l app=motel-booking-app

# Pod details
kubectl describe pod POD_NAME -n production

# Logs
kubectl logs -f deployment/motel-booking-app -n production

# Multiple pods
kubectl logs -f -l app=motel-booking-app -n production --all-containers=true

# Events
kubectl get events -n production --sort-by='.lastTimestamp'
```

### Debugging

```bash
# Execute command in pod
kubectl exec -it POD_NAME -n production -- /bin/sh

# Port forward for local access
kubectl port-forward svc/motel-booking-service 8080:80 -n production

# Get pod metrics
kubectl top pods -n production
kubectl top nodes
```

---

## Security

### Network Policies

**k8s/networkpolicy.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: motel-booking-network-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: motel-booking-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 6379  # Redis
    - protocol: TCP
      port: 443   # HTTPS
```

### Pod Security Standards

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

---

## High Availability

### Pod Disruption Budget

**k8s/pdb.yaml:**
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: motel-booking-pdb
  namespace: production
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app: motel-booking-app
```

### Anti-Affinity

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - motel-booking-app
        topologyKey: kubernetes.io/hostname
```

---

## Troubleshooting

### Pod Won't Start

```bash
# Check pod status
kubectl describe pod POD_NAME -n production

# Common issues:
# - ImagePullBackOff: Check image name and registry credentials
# - CrashLoopBackOff: Check application logs
# - Pending: Check resource availability
```

### Health Check Failures

```bash
# Test health endpoint from within cluster
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://motel-booking-service.production/actuator/health

# Adjust probe timings if needed
kubectl edit deployment/motel-booking-app -n production
```

### Performance Issues

```bash
# Check resource usage
kubectl top pods -n production

# Check HPA status
kubectl describe hpa motel-booking-hpa -n production

# Review metrics
kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/production/pods
```

---

## Best Practices

1. **Use namespaces** to isolate environments
2. **Set resource limits** for all containers
3. **Implement health checks** (liveness and readiness)
4. **Use ConfigMaps** for configuration
5. **Never commit secrets** to version control
6. **Use Pod Disruption Budgets** for HA
7. **Implement network policies** for security
8. **Tag all images** with specific versions
9. **Use rolling updates** for zero-downtime
10. **Monitor resource usage** continuously

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0
