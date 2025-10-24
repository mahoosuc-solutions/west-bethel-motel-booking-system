# Deployment Quick Start Guide

## 🚀 Get Started in 5 Minutes

This quick reference guide gets you deploying fast. For detailed instructions, see `docs/deployment/`.

---

## Prerequisites Checklist

```bash
✓ kubectl installed and configured
✓ Docker installed
✓ AWS/GCP credentials configured
✓ GitHub repository access
✓ Database credentials ready
```

---

## Quick Commands

### Deploy to Staging

```bash
# Automated (via GitHub)
git push origin develop

# Manual
./scripts/deploy.sh --environment staging --tag latest
```

### Deploy to Production

```bash
# Create version tag (triggers auto-deploy)
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

### Health Check

```bash
./scripts/health-check.sh https://www.westbethelmotel.com
```

### Smoke Tests

```bash
./scripts/smoke-test.sh https://www.westbethelmotel.com
```

### Emergency Rollback

```bash
./scripts/rollback.sh \
  --environment production \
  --reason "Critical bug description"
```

---

## Infrastructure Setup (One-Time)

### AWS Infrastructure

```bash
cd terraform/aws

# Initialize
terraform init

# Create production infrastructure
terraform apply -var-file=environments/production/terraform.tfvars

# Note the outputs (DB endpoint, ALB DNS, etc.)
```

### Kubernetes Setup

```bash
# Configure kubectl
aws eks update-kubeconfig --name motel-booking-cluster --region us-east-1

# Create secrets
kubectl create secret generic db-credentials \
  --from-literal=url=jdbc:postgresql://DB_HOST:5432/motel_booking \
  --from-literal=username=moteluser \
  --from-literal=password=YOUR_PASSWORD \
  -n production

kubectl create secret generic jwt-secret \
  --from-literal=secret=$(openssl rand -base64 64) \
  -n production

# Deploy application
kubectl apply -f k8s/
```

---

## GitHub Secrets to Configure

### Required Secrets

```yaml
# AWS
AWS_ACCESS_KEY_ID: Your AWS access key
AWS_SECRET_ACCESS_KEY: Your AWS secret key
AWS_REGION: us-east-1

# Database
STAGING_DB_URL: jdbc:postgresql://staging-db:5432/db
STAGING_DB_USER: moteluser
STAGING_DB_PASSWORD: [password]
PROD_DB_URL: jdbc:postgresql://prod-db:5432/db
PROD_DB_USER: moteluser
PROD_DB_PASSWORD: [password]

# Application
JWT_SECRET: [64-char-random-string]

# Email
SMTP_HOST: smtp.gmail.com
SMTP_USERNAME: noreply@westbethelmotel.com
SMTP_PASSWORD: [app-password]

# Notifications (optional)
SLACK_WEBHOOK_URL: https://hooks.slack.com/services/...
```

### Configure in GitHub

```
Repository → Settings → Secrets and variables → Actions → New repository secret
```

---

## Kubernetes Quick Operations

### View Status

```bash
# Pods
kubectl get pods -n production

# Deployments
kubectl get deployments -n production

# Services
kubectl get services -n production
```

### View Logs

```bash
# All pods
kubectl logs -f -l app=motel-booking-app -n production

# Specific pod
kubectl logs -f POD_NAME -n production
```

### Scale Application

```bash
# Manual scaling
kubectl scale deployment/motel-booking-app --replicas=5 -n production

# Check auto-scaling
kubectl get hpa -n production
```

### Update Application

```bash
# Set new image
kubectl set image deployment/motel-booking-app \
  motel-booking-app=ghcr.io/westbethel/motel-booking-system:v1.1.0 \
  -n production

# Watch rollout
kubectl rollout status deployment/motel-booking-app -n production
```

### Rollback

```bash
# Rollback to previous
kubectl rollout undo deployment/motel-booking-app -n production

# Rollback to specific revision
kubectl rollout undo deployment/motel-booking-app --to-revision=3 -n production
```

---

## Common Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod POD_NAME -n production

# Check logs
kubectl logs POD_NAME -n production --previous
```

### Database Connection Issues

```bash
# Test from pod
kubectl exec -it POD_NAME -n production -- \
  psql -h $DB_HOST -U $DB_USER -c "SELECT 1"

# Check secrets
kubectl get secret db-credentials -n production -o yaml
```

### Health Check Failures

```bash
# Test health endpoint
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://motel-booking-service.production/actuator/health
```

---

## Monitoring URLs

```
Application:    https://www.westbethelmotel.com
Staging:        https://staging.westbethelmotel.com
Grafana:        https://grafana.westbethelmotel.com
Prometheus:     https://prometheus.westbethelmotel.com
Kibana:         https://kibana.westbethelmotel.com
```

---

## Emergency Contacts

```
On-Call:        oncall@westbethelmotel.com
Slack:          #incidents
PagerDuty:      https://westbethelmotel.pagerduty.com
```

---

## Documentation Index

### For Deployment
- **Complete Guide:** `docs/deployment/DEPLOYMENT_GUIDE.md`
- **Kubernetes:** `docs/deployment/KUBERNETES_GUIDE.md`
- **Infrastructure:** `docs/deployment/INFRASTRUCTURE_GUIDE.md`

### For CI/CD
- **Pipeline Setup:** `docs/deployment/CI_CD_SETUP.md`

### For Operations
- **Rollback:** `docs/deployment/ROLLBACK_GUIDE.md`
- **Monitoring:** `docs/deployment/MONITORING_RUNBOOK.md`

### For Environment Setup
- **Variables:** `docs/ENVIRONMENT_VARIABLES.md`
- **Security:** `docs/SECURITY_TESTING.md`

---

## File Locations

```
Scripts:          scripts/
  ├── deploy.sh           # Main deployment
  ├── rollback.sh         # Emergency rollback
  ├── health-check.sh     # Health verification
  ├── smoke-test.sh       # Smoke tests
  ├── backup-database.sh  # Database backup
  └── restore.sh          # Database restore

Kubernetes:       k8s/
  ├── deployment.yaml     # Application deployment
  ├── service.yaml        # Service definition
  ├── ingress.yaml        # Ingress/routing
  ├── hpa.yaml           # Auto-scaling
  └── [8 more files]

Infrastructure:   terraform/
  ├── aws/               # AWS infrastructure
  └── [GCP existing]     # GCP infrastructure

Workflows:        .github/workflows/
  ├── ci.yml             # CI pipeline
  ├── cd-staging.yml     # Staging deploy
  ├── cd-production.yml  # Production deploy
  └── rollback.yml       # Emergency rollback
```

---

## Success Criteria

After deployment, verify:

```bash
✓ Health check passes:     ./scripts/health-check.sh [URL]
✓ Smoke tests pass:        ./scripts/smoke-test.sh [URL]
✓ All pods running:        kubectl get pods -n production
✓ Metrics available:       curl [URL]/actuator/metrics
✓ No critical alerts:      Check Grafana/CloudWatch
```

---

## Next Steps

1. ✅ Review documentation in `docs/deployment/`
2. ✅ Configure GitHub Secrets
3. ✅ Provision infrastructure with Terraform
4. ✅ Deploy to staging and test
5. ✅ Deploy to production with monitoring

---

**Need Help?**
- Full documentation: `docs/deployment/`
- Emergency procedures: `docs/deployment/ROLLBACK_GUIDE.md`
- Operations guide: `docs/deployment/MONITORING_RUNBOOK.md`

**Ready to deploy!** 🚀
