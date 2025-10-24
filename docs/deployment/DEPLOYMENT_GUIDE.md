# West Bethel Motel Booking System - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Deployment Architectures](#deployment-architectures)
4. [Environment Configuration](#environment-configuration)
5. [Deployment Procedures](#deployment-procedures)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Troubleshooting](#troubleshooting)
8. [Rollback Procedures](#rollback-procedures)

---

## Overview

This guide provides comprehensive instructions for deploying the West Bethel Motel Booking System to production and staging environments. The system supports multiple deployment strategies:

- **Kubernetes** (EKS, GKE, or self-managed)
- **AWS ECS** (Elastic Container Service)
- **Docker Compose** (for development/testing)

### Deployment Philosophy

Our deployment strategy emphasizes:
- **Zero-downtime deployments** using rolling updates
- **Blue-green deployments** for production
- **Automated health checks** and rollback capabilities
- **Infrastructure as Code** for reproducibility
- **Comprehensive monitoring** and alerting

---

## Prerequisites

### Required Tools

```bash
# Kubernetes deployments
kubectl >= 1.27
helm >= 3.12

# AWS deployments
aws-cli >= 2.13
terraform >= 1.5

# Container management
docker >= 24.0
docker-compose >= 2.20

# Utilities
jq >= 1.6
curl >= 8.0
git >= 2.40
```

### Access Requirements

1. **Cloud Provider Access**
   - AWS: IAM credentials with appropriate permissions
   - GCP: Service account with required roles
   - Kubernetes: kubeconfig with cluster access

2. **Container Registry Access**
   - GitHub Container Registry (ghcr.io)
   - Or your organization's private registry

3. **Secrets and Credentials**
   - Database credentials
   - JWT signing keys
   - SMTP credentials
   - API keys

---

## Deployment Architectures

### Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Internet                            │
└────────────────────┬────────────────────────────────────┘
                     │
              ┌──────▼──────┐
              │   Ingress   │ (SSL/TLS Termination)
              │  Controller │
              └──────┬──────┘
                     │
        ┌────────────┴────────────┐
        │                         │
   ┌────▼────┐              ┌────▼────┐
   │ Service │              │ Service │
   │  (App)  │              │ (Mgmt)  │
   └────┬────┘              └────┬────┘
        │                         │
   ┌────▼──────────┐         ┌───▼──────┐
   │  Deployment   │         │   HPA    │
   │  (2-10 pods)  │         │          │
   └───────┬───────┘         └──────────┘
           │
   ┌───────┴────────┐
   │                │
┌──▼───┐      ┌────▼────┐
│  RDS │      │  Redis  │
│ (HA) │      │ (Cache) │
└──────┘      └─────────┘
```

### AWS ECS Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Route 53                              │
└────────────────────┬────────────────────────────────────┘
                     │
              ┌──────▼──────┐
              │     ALB     │ (SSL/TLS, WAF)
              │             │
              └──────┬──────┘
                     │
        ┌────────────┴────────────┐
        │                         │
   ┌────▼────┐              ┌────▼────┐
   │   ECS   │              │   ECS   │
   │ Service │              │ Service │
   │  (App)  │              │ (Jobs)  │
   └────┬────┘              └─────────┘
        │
   ┌────▼──────────┐
   │  ECS Tasks    │
   │  (2-10 tasks) │
   └───────┬───────┘
           │
   ┌───────┴────────┐
   │                │
┌──▼───┐      ┌────▼────────┐
│  RDS │      │ ElastiCache │
│ (HA) │      │   (Redis)   │
└──────┘      └─────────────┘
```

---

## Environment Configuration

### Environment Variables

Each environment requires specific configuration. See `docs/ENVIRONMENT_VARIABLES.md` for complete reference.

#### Critical Environment Variables

```bash
# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
MANAGEMENT_PORT=8081

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://[HOST]:5432/[DB_NAME]
SPRING_DATASOURCE_USERNAME=[USERNAME]
SPRING_DATASOURCE_PASSWORD=[PASSWORD]

# Redis
SPRING_REDIS_HOST=[REDIS_HOST]
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=[PASSWORD]

# Security
JWT_SECRET=[64_CHAR_SECRET]
MFA_ENABLED=true

# Email
SPRING_MAIL_HOST=[SMTP_HOST]
SPRING_MAIL_USERNAME=[USERNAME]
SPRING_MAIL_PASSWORD=[PASSWORD]

# Monitoring
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
MANAGEMENT_TRACING_ENABLED=true
```

### Secrets Management

**Production:** Use cloud provider's secrets management
- AWS: AWS Secrets Manager or Parameter Store
- GCP: Secret Manager
- Kubernetes: Sealed Secrets or External Secrets Operator

**Example: AWS Secrets Manager**
```bash
# Store database password
aws secretsmanager create-secret \
  --name production/db/password \
  --secret-string "your-secure-password"

# Store JWT secret
aws secretsmanager create-secret \
  --name production/jwt/secret \
  --secret-string "your-64-char-jwt-secret"
```

---

## Deployment Procedures

### Kubernetes Deployment

#### 1. Prepare Infrastructure

```bash
# Navigate to terraform directory
cd terraform/

# Initialize Terraform
terraform init

# Review plan
terraform plan -var-file=environments/production/terraform.tfvars

# Apply infrastructure
terraform apply -var-file=environments/production/terraform.tfvars
```

#### 2. Configure kubectl

```bash
# AWS EKS
aws eks update-kubeconfig --name motel-booking-cluster --region us-east-1

# GCP GKE
gcloud container clusters get-credentials motel-booking-cluster --region us-central1

# Verify connection
kubectl cluster-info
kubectl get nodes
```

#### 3. Create Namespace and Secrets

```bash
# Create namespace
kubectl create namespace production

# Create secrets (from existing files or Sealed Secrets)
kubectl create secret generic db-credentials \
  --from-literal=username=moteluser \
  --from-literal=password=YOUR_PASSWORD \
  -n production

kubectl create secret generic jwt-secret \
  --from-literal=secret=YOUR_JWT_SECRET \
  -n production

kubectl create secret generic smtp-credentials \
  --from-literal=username=YOUR_SMTP_USER \
  --from-literal=password=YOUR_SMTP_PASS \
  -n production
```

#### 4. Deploy Application

```bash
# Apply all Kubernetes manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/serviceaccount.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/pdb.yaml
kubectl apply -f k8s/networkpolicy.yaml

# Or use the deployment script
./scripts/deploy.sh --environment production --tag v1.0.0
```

#### 5. Monitor Deployment

```bash
# Watch deployment progress
kubectl rollout status deployment/motel-booking-app -n production

# Check pods
kubectl get pods -n production -l app=motel-booking-app

# View logs
kubectl logs -f deployment/motel-booking-app -n production

# Check events
kubectl get events -n production --sort-by='.lastTimestamp'
```

### AWS ECS Deployment

#### 1. Build and Push Container

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin [ACCOUNT].dkr.ecr.us-east-1.amazonaws.com

# Build image
docker build -f Dockerfile.production -t motel-booking:v1.0.0 .

# Tag for ECR
docker tag motel-booking:v1.0.0 \
  [ACCOUNT].dkr.ecr.us-east-1.amazonaws.com/motel-booking:v1.0.0

# Push to ECR
docker push [ACCOUNT].dkr.ecr.us-east-1.amazonaws.com/motel-booking:v1.0.0
```

#### 2. Deploy to ECS

```bash
# Update ECS service with new task definition
aws ecs update-service \
  --cluster motel-booking-cluster \
  --service motel-booking-service \
  --task-definition motel-booking:v1.0.0 \
  --force-new-deployment

# Monitor deployment
aws ecs wait services-stable \
  --cluster motel-booking-cluster \
  --services motel-booking-service
```

---

## Post-Deployment Verification

### Health Checks

```bash
# Run health check script
./scripts/health-check.sh https://www.westbethelmotel.com

# Manual health check
curl https://www.westbethelmotel.com/actuator/health

# Check readiness
curl https://www.westbethelmotel.com/actuator/health/readiness

# Check liveness
curl https://www.westbethelmotel.com/actuator/health/liveness
```

### Smoke Tests

```bash
# Run automated smoke tests
./scripts/smoke-test.sh https://www.westbethelmotel.com

# Manual API test
curl -X POST https://www.westbethelmotel.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

### Monitoring Verification

```bash
# Check metrics endpoint
curl https://www.westbethelmotel.com/actuator/metrics

# Check Prometheus metrics
curl https://www.westbethelmotel.com/actuator/prometheus

# Verify logs are flowing
kubectl logs -f deployment/motel-booking-app -n production --tail=100
```

---

## Troubleshooting

### Common Issues

#### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n production

# Describe pod for events
kubectl describe pod [POD_NAME] -n production

# Check logs
kubectl logs [POD_NAME] -n production

# Common causes:
# - Image pull errors (check registry credentials)
# - Missing secrets
# - Insufficient resources
# - Health check failures
```

#### Database Connection Failures

```bash
# Test database connectivity from pod
kubectl exec -it [POD_NAME] -n production -- \
  psql -h $SPRING_DATASOURCE_URL -U $SPRING_DATASOURCE_USERNAME

# Check network policies
kubectl get networkpolicies -n production

# Verify secrets
kubectl get secret db-credentials -n production -o yaml
```

#### SSL/TLS Issues

```bash
# Check ingress configuration
kubectl describe ingress -n production

# Verify certificate
echo | openssl s_client -servername www.westbethelmotel.com -connect www.westbethelmotel.com:443

# Check cert-manager (if used)
kubectl get certificates -n production
```

### Debug Mode

Enable verbose logging temporarily:

```bash
# Update deployment with debug logging
kubectl set env deployment/motel-booking-app \
  LOGGING_LEVEL_ROOT=DEBUG \
  -n production

# Revert after debugging
kubectl set env deployment/motel-booking-app \
  LOGGING_LEVEL_ROOT=INFO \
  -n production
```

---

## Rollback Procedures

### Automated Rollback

```bash
# Using rollback script
./scripts/rollback.sh \
  --environment production \
  --reason "Critical bug in v1.0.1"

# Rollback to specific version
./scripts/rollback.sh \
  --environment production \
  --version v1.0.0 \
  --reason "Reverting to last stable version"
```

### Manual Rollback (Kubernetes)

```bash
# View rollout history
kubectl rollout history deployment/motel-booking-app -n production

# Rollback to previous version
kubectl rollout undo deployment/motel-booking-app -n production

# Rollback to specific revision
kubectl rollout undo deployment/motel-booking-app -n production --to-revision=3

# Monitor rollback
kubectl rollout status deployment/motel-booking-app -n production
```

### Manual Rollback (ECS)

```bash
# List task definitions
aws ecs list-task-definitions --family-prefix motel-booking

# Update service to previous task definition
aws ecs update-service \
  --cluster motel-booking-cluster \
  --service motel-booking-service \
  --task-definition motel-booking:v1.0.0
```

### Post-Rollback Actions

1. **Verify Application Health**
   ```bash
   ./scripts/health-check.sh https://www.westbethelmotel.com
   ./scripts/smoke-test.sh https://www.westbethelmotel.com
   ```

2. **Create Incident Report**
   - Document the issue
   - Record rollback time
   - Identify root cause
   - Plan remediation

3. **Notify Stakeholders**
   - Alert team via Slack/email
   - Update status page
   - Communicate timeline for fix

---

## Best Practices

1. **Always test in staging first**
2. **Use feature flags for gradual rollouts**
3. **Monitor closely for the first hour after deployment**
4. **Keep rollback plans ready**
5. **Document all production changes**
6. **Maintain backup before major deployments**
7. **Use blue-green deployments for critical updates**
8. **Automate as much as possible**

---

## Additional Resources

- [CI/CD Setup Guide](CI_CD_SETUP.md)
- [Infrastructure Guide](INFRASTRUCTURE_GUIDE.md)
- [Kubernetes Guide](KUBERNETES_GUIDE.md)
- [Rollback Guide](ROLLBACK_GUIDE.md)
- [Monitoring Runbook](MONITORING_RUNBOOK.md)
- [Environment Variables Reference](../ENVIRONMENT_VARIABLES.md)

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0
**Maintained By:** DevOps Team
