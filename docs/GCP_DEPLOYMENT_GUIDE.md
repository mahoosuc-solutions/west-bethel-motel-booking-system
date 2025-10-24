# Google Cloud Platform Deployment Guide
# West Bethel Motel Booking System

**Last Updated**: October 24, 2025
**Target Platform**: Google Cloud Run
**Database**: Cloud SQL (PostgreSQL 15)
**Cache**: Memorystore for Redis

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [GCP Project Setup](#gcp-project-setup)
3. [Enable Required APIs](#enable-required-apis)
4. [Create Service Account](#create-service-account)
5. [Cloud SQL Setup](#cloud-sql-setup)
6. [Memorystore Redis Setup](#memorystore-redis-setup)
7. [VPC Configuration](#vpc-configuration)
8. [Secret Manager Setup](#secret-manager-setup)
9. [Container Registry](#container-registry)
10. [Cloud Build Setup](#cloud-build-setup)
11. [Cloud Run Deployment](#cloud-run-deployment)
12. [Environment Variables](#environment-variables)
13. [Database Migration](#database-migration)
14. [Monitoring and Logging](#monitoring-and-logging)
15. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Local Tools Required

```bash
# Install Google Cloud SDK
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Initialize gcloud
gcloud init

# Install additional components
gcloud components install cloud-build-local cloud-run-proxy
```

### Required Accounts
- Google Cloud Platform account with billing enabled
- GitHub repository access (for CI/CD)
- Domain name (optional, for custom domain)

---

## GCP Project Setup

### 1. Create New Project

```bash
# Set project variables
export GCP_PROJECT_ID="westbethel-motel-prod"
export GCP_REGION="us-central1"
export GCP_ZONE="us-central1-a"

# Create project
gcloud projects create $GCP_PROJECT_ID \
  --name="West Bethel Motel Booking System"

# Set as default project
gcloud config set project $GCP_PROJECT_ID

# Link billing account (replace BILLING_ACCOUNT_ID)
gcloud billing accounts list
gcloud billing projects link $GCP_PROJECT_ID \
  --billing-account=BILLING_ACCOUNT_ID
```

---

## Enable Required APIs

```bash
# Enable all required GCP APIs
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  redis.googleapis.com \
  vpcaccess.googleapis.com \
  secretmanager.googleapis.com \
  containerregistry.googleapis.com \
  cloudresourcemanager.googleapis.com \
  iam.googleapis.com \
  monitoring.googleapis.com \
  logging.googleapis.com \
  cloudtrace.googleapis.com
```

---

## Create Service Account

```bash
# Create service account for Cloud Run
export SERVICE_ACCOUNT_NAME="motel-booking-sa"
export SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create $SERVICE_ACCOUNT_NAME \
  --display-name="Motel Booking System Service Account"

# Grant necessary roles
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/cloudsql.client"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/secretmanager.secretAccessor"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/logging.logWriter"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/cloudtrace.agent"

gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/monitoring.metricWriter"
```

---

## Cloud SQL Setup

### 1. Create PostgreSQL Instance

```bash
# Set Cloud SQL variables
export CLOUD_SQL_INSTANCE="motel-booking-db"
export CLOUD_SQL_CONNECTION_NAME="${GCP_PROJECT_ID}:${GCP_REGION}:${CLOUD_SQL_INSTANCE}"
export DB_NAME="motel_booking"
export DB_USERNAME="motel_app"
export DB_PASSWORD=$(openssl rand -base64 32)  # Generate secure password

# Create Cloud SQL instance (PostgreSQL 15)
gcloud sql instances create $CLOUD_SQL_INSTANCE \
  --database-version=POSTGRES_15 \
  --tier=db-custom-2-7680 \
  --region=$GCP_REGION \
  --storage-type=SSD \
  --storage-size=20GB \
  --storage-auto-increase \
  --backup \
  --backup-start-time=03:00 \
  --maintenance-window-day=SUN \
  --maintenance-window-hour=4 \
  --availability-type=REGIONAL \
  --enable-point-in-time-recovery \
  --retained-backups-count=7 \
  --transaction-log-retention-days=7

# Create database
gcloud sql databases create $DB_NAME \
  --instance=$CLOUD_SQL_INSTANCE

# Create user
gcloud sql users create $DB_USERNAME \
  --instance=$CLOUD_SQL_INSTANCE \
  --password=$DB_PASSWORD

# Save connection details (IMPORTANT!)
echo "Cloud SQL Connection Name: $CLOUD_SQL_CONNECTION_NAME"
echo "Database Name: $DB_NAME"
echo "Database Username: $DB_USERNAME"
echo "Database Password: $DB_PASSWORD"
# SAVE THESE CREDENTIALS SECURELY!
```

### 2. Configure Networking

```bash
# Enable Private IP (recommended for production)
gcloud sql instances patch $CLOUD_SQL_INSTANCE \
  --network=projects/$GCP_PROJECT_ID/global/networks/default \
  --enable-private-ip

# OR allow public IP with authorized networks (less secure)
gcloud sql instances patch $CLOUD_SQL_INSTANCE \
  --authorized-networks=0.0.0.0/0
```

---

## Memorystore Redis Setup

```bash
# Create Redis instance
export REDIS_INSTANCE="motel-booking-cache"

gcloud redis instances create $REDIS_INSTANCE \
  --size=1 \
  --region=$GCP_REGION \
  --tier=standard \
  --redis-version=redis_7_0 \
  --network=default \
  --transit-encryption-mode=SERVER_AUTHENTICATION

# Get Redis connection details
gcloud redis instances describe $REDIS_INSTANCE --region=$GCP_REGION

# Save these values:
export REDIS_HOST=$(gcloud redis instances describe $REDIS_INSTANCE --region=$GCP_REGION --format="value(host)")
export REDIS_PORT=$(gcloud redis instances describe $REDIS_INSTANCE --region=$GCP_REGION --format="value(port)")
export REDIS_AUTH=$(gcloud redis instances describe $REDIS_INSTANCE --region=$GCP_REGION --format="value(authString)")

echo "Redis Host: $REDIS_HOST"
echo "Redis Port: $REDIS_PORT"
echo "Redis Auth: $REDIS_AUTH"
```

---

## VPC Configuration

### 1. Create Serverless VPC Connector

```bash
# Create VPC connector for Cloud Run to access Cloud SQL and Redis
export VPC_CONNECTOR="motel-booking-connector"

gcloud compute networks vpc-access connectors create $VPC_CONNECTOR \
  --region=$GCP_REGION \
  --network=default \
  --range=10.8.0.0/28 \
  --min-instances=2 \
  --max-instances=10 \
  --machine-type=e2-micro

# Verify creation
gcloud compute networks vpc-access connectors describe $VPC_CONNECTOR \
  --region=$GCP_REGION
```

---

## Secret Manager Setup

### 1. Create Secrets

```bash
# Create secrets in Secret Manager
echo -n "$DB_USERNAME" | gcloud secrets create db-username \
  --data-file=- \
  --replication-policy="automatic"

echo -n "$DB_PASSWORD" | gcloud secrets create db-password \
  --data-file=- \
  --replication-policy="automatic"

echo -n "$REDIS_AUTH" | gcloud secrets create redis-password \
  --data-file=- \
  --replication-policy="automatic"

# Generate JWT secret (256-bit minimum)
export JWT_SECRET=$(openssl rand -base64 64)
echo -n "$JWT_SECRET" | gcloud secrets create jwt-secret \
  --data-file=- \
  --replication-policy="automatic"

# Email credentials (use your SMTP provider)
echo -n "your-email@gmail.com" | gcloud secrets create mail-username \
  --data-file=- \
  --replication-policy="automatic"

echo -n "your-app-password" | gcloud secrets create mail-password \
  --data-file=- \
  --replication-policy="automatic"

# Grant service account access to secrets
for SECRET in db-username db-password redis-password jwt-secret mail-username mail-password; do
  gcloud secrets add-iam-policy-binding $SECRET \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/secretmanager.secretAccessor"
done
```

---

## Container Registry

### 1. Configure Docker Authentication

```bash
# Configure Docker to use gcloud as credential helper
gcloud auth configure-docker

# Build and test image locally
docker build -t gcr.io/$GCP_PROJECT_ID/motel-booking-system:test \
  -f Dockerfile.production \
  --build-arg SKIP_TESTS=false \
  .

# Push test image
docker push gcr.io/$GCP_PROJECT_ID/motel-booking-system:test
```

---

## Cloud Build Setup

### 1. Grant Cloud Build Permissions

```bash
# Get Cloud Build service account
export CLOUDBUILD_SA="$(gcloud projects describe $GCP_PROJECT_ID \
  --format='value(projectNumber)')@cloudbuild.gserviceaccount.com"

# Grant Cloud Run Admin role
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${CLOUDBUILD_SA}" \
  --role="roles/run.admin"

# Grant Service Account User role
gcloud iam service-accounts add-iam-policy-binding \
  $SERVICE_ACCOUNT_EMAIL \
  --member="serviceAccount:${CLOUDBUILD_SA}" \
  --role="roles/iam.serviceAccountUser"

# Grant Secret Manager Access
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:${CLOUDBUILD_SA}" \
  --role="roles/secretmanager.secretAccessor"
```

### 2. Create Cloud Build Trigger

```bash
# Create trigger for main branch
gcloud builds triggers create github \
  --repo-name=west-bethel-motel-booking-system \
  --repo-owner=YOUR_GITHUB_ORG \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml \
  --substitutions=\
_REGION=$GCP_REGION,\
_CLOUD_SQL_CONNECTION_NAME=$CLOUD_SQL_CONNECTION_NAME,\
_SERVICE_ACCOUNT=$SERVICE_ACCOUNT_EMAIL,\
_VPC_CONNECTOR=projects/$GCP_PROJECT_ID/locations/$GCP_REGION/connectors/$VPC_CONNECTOR

# Test manual build
gcloud builds submit \
  --config=cloudbuild.yaml \
  --substitutions=\
_REGION=$GCP_REGION,\
_CLOUD_SQL_CONNECTION_NAME=$CLOUD_SQL_CONNECTION_NAME,\
_SERVICE_ACCOUNT=$SERVICE_ACCOUNT_EMAIL,\
_VPC_CONNECTOR=projects/$GCP_PROJECT_ID/locations/$GCP_REGION/connectors/$VPC_CONNECTOR
```

---

## Cloud Run Deployment

### Manual Deployment

```bash
# Deploy to Cloud Run
gcloud run deploy motel-booking-system \
  --image=gcr.io/$GCP_PROJECT_ID/motel-booking-system:latest \
  --region=$GCP_REGION \
  --platform=managed \
  --allow-unauthenticated \
  --memory=1Gi \
  --cpu=1 \
  --timeout=300 \
  --concurrency=80 \
  --min-instances=1 \
  --max-instances=10 \
  --port=8080 \
  --set-env-vars="\
SPRING_PROFILES_ACTIVE=gcp,\
GCP_PROJECT_ID=$GCP_PROJECT_ID,\
CLOUD_SQL_CONNECTION_NAME=$CLOUD_SQL_CONNECTION_NAME,\
CLOUD_SQL_DATABASE_NAME=$DB_NAME,\
REDIS_HOST=$REDIS_HOST,\
REDIS_PORT=$REDIS_PORT" \
  --add-cloudsql-instances=$CLOUD_SQL_CONNECTION_NAME \
  --set-secrets="\
DATABASE_USERNAME=db-username:latest,\
DATABASE_PASSWORD=db-password:latest,\
JWT_SECRET=jwt-secret:latest,\
REDIS_PASSWORD=redis-password:latest,\
MAIL_USERNAME=mail-username:latest,\
MAIL_PASSWORD=mail-password:latest" \
  --service-account=$SERVICE_ACCOUNT_EMAIL \
  --vpc-connector=$VPC_CONNECTOR \
  --vpc-egress=private-ranges-only

# Get service URL
export SERVICE_URL=$(gcloud run services describe motel-booking-system \
  --region=$GCP_REGION \
  --format='value(status.url)')

echo "Service URL: $SERVICE_URL"
```

---

## Environment Variables

### Required Environment Variables

```bash
# Spring Boot
SPRING_PROFILES_ACTIVE=gcp

# GCP Configuration
GCP_PROJECT_ID=<your-project-id>
CLOUD_SQL_CONNECTION_NAME=<project>:<region>:<instance>
CLOUD_SQL_DATABASE_NAME=motel_booking

# Database (from Secret Manager)
DATABASE_USERNAME=<secret>
DATABASE_PASSWORD=<secret>

# Redis
REDIS_HOST=<memorystore-ip>
REDIS_PORT=6379
REDIS_PASSWORD=<secret>

# JWT (from Secret Manager)
JWT_SECRET=<secret>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Email (from Secret Manager)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=<secret>
MAIL_PASSWORD=<secret>

# Application Server
PORT=8080
SERVER_PORT=8080
```

---

## Database Migration

### Run Flyway Migrations

```bash
# Connect via Cloud SQL Proxy
cloud_sql_proxy -instances=$CLOUD_SQL_CONNECTION_NAME=tcp:5432 &

# Run migrations locally
export DATABASE_URL="jdbc:postgresql://localhost:5432/$DB_NAME"
export DATABASE_USERNAME="$DB_USERNAME"
export DATABASE_PASSWORD="$DB_PASSWORD"

mvn flyway:migrate \
  -Dflyway.url=$DATABASE_URL \
  -Dflyway.user=$DATABASE_USERNAME \
  -Dflyway.password=$DATABASE_PASSWORD

# Or deploy with migrations enabled
# Migrations will run automatically on first deployment
```

---

## Monitoring and Logging

### 1. Cloud Logging

```bash
# View application logs
gcloud logging read "resource.type=cloud_run_revision \
  AND resource.labels.service_name=motel-booking-system" \
  --limit=50 \
  --format=json

# Tail logs in real-time
gcloud alpha logging tail "resource.type=cloud_run_revision \
  AND resource.labels.service_name=motel-booking-system"
```

### 2. Cloud Monitoring

```bash
# Create uptime check
gcloud monitoring uptime-checks create \
  motel-booking-health-check \
  --display-name="Motel Booking Health Check" \
  --resource-type=uptime-url \
  --host=$SERVICE_URL \
  --path=/actuator/health/readiness
```

### 3. Prometheus Metrics

```bash
# Access Prometheus metrics
curl $SERVICE_URL/actuator/prometheus
```

---

## Troubleshooting

### Common Issues

#### 1. Cloud SQL Connection Timeout

```bash
# Check VPC connector status
gcloud compute networks vpc-access connectors describe $VPC_CONNECTOR \
  --region=$GCP_REGION

# Verify service account has cloudsql.client role
gcloud projects get-iam-policy $GCP_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:$SERVICE_ACCOUNT_EMAIL"
```

#### 2. Secret Access Denied

```bash
# Grant secret access explicitly
gcloud secrets add-iam-policy-binding SECRET_NAME \
  --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
  --role="roles/secretmanager.secretAccessor"
```

#### 3. Container Startup Failures

```bash
# Check container logs
gcloud logging read "resource.type=cloud_run_revision \
  AND resource.labels.service_name=motel-booking-system \
  AND severity>=ERROR" \
  --limit=100

# Test container locally
docker run -it --rm \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=gcp \
  -e GCP_PROJECT_ID=$GCP_PROJECT_ID \
  gcr.io/$GCP_PROJECT_ID/motel-booking-system:latest
```

#### 4. Health Check Failures

```bash
# Test health endpoint
curl $SERVICE_URL/actuator/health/liveness
curl $SERVICE_URL/actuator/health/readiness

# Check startup time
gcloud run services describe motel-booking-system \
  --region=$GCP_REGION \
  --format="value(spec.template.spec.containers[0].startupProbe)"
```

---

## Cost Optimization

### 1. Cloud Run

```bash
# Use minimum instances=0 for dev/staging
gcloud run services update motel-booking-system \
  --region=$GCP_REGION \
  --min-instances=0

# Use smaller CPU allocation
gcloud run services update motel-booking-system \
  --region=$GCP_REGION \
  --cpu=1
```

### 2. Cloud SQL

```bash
# Use smaller instance for non-prod
gcloud sql instances patch $CLOUD_SQL_INSTANCE \
  --tier=db-f1-micro  # For dev/test only
```

---

## Security Checklist

- [ ] All secrets stored in Secret Manager
- [ ] Service account follows least-privilege principle
- [ ] VPC connector configured for private network access
- [ ] Cloud SQL uses private IP
- [ ] Redis authentication enabled
- [ ] HTTPS enforced on Cloud Run
- [ ] Regular security updates applied
- [ ] Audit logging enabled
- [ ] Backup and disaster recovery configured
- [ ] Database encryption at rest enabled
- [ ] Application-level authentication (JWT) configured

---

## Next Steps

1. Configure custom domain with Cloud Run
2. Set up Cloud CDN for static assets
3. Configure Cloud Armor for DDoS protection
4. Set up multi-region deployment
5. Configure Cloud Build notification emails
6. Set up Grafana dashboards for metrics
7. Configure alerting policies

---

## Support

**Repository**: https://github.com/mahoosuc-solutions/west-bethel-motel-booking-system
**Documentation**: `docs/`
**Issues**: GitHub Issues

---

**Generated**: October 24, 2025
**Version**: 1.0.0
