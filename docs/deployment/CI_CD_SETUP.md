# CI/CD Pipeline Setup Guide

## Overview

This guide explains how to set up and configure the CI/CD pipelines for the West Bethel Motel Booking System using GitHub Actions.

## Table of Contents

1. [Pipeline Overview](#pipeline-overview)
2. [Prerequisites](#prerequisites)
3. [GitHub Secrets Configuration](#github-secrets-configuration)
4. [Pipeline Workflows](#pipeline-workflows)
5. [Deployment Environments](#deployment-environments)
6. [Troubleshooting](#troubleshooting)

---

## Pipeline Overview

Our CI/CD pipeline consists of 5 main workflows:

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD Pipeline Flow                       │
└─────────────────────────────────────────────────────────────┘

1. ci.yml
   ├── Code Quality Check
   ├── Unit Tests
   ├── Integration Tests
   ├── Security Scan
   └── Build Docker Image
        │
        ▼
2. security-scan.yml (Daily + on Push)
   ├── OWASP Dependency Check
   ├── Trivy Container Scan
   ├── Snyk Security Scan
   ├── CodeQL Analysis
   └── GitLeaks Secret Scan
        │
        ▼
3. cd-staging.yml (on develop branch)
   ├── Run Tests
   ├── Build & Push Image
   ├── Database Migration
   ├── Deploy to Kubernetes
   ├── Health Checks
   └── Smoke Tests
        │
        ▼
4. cd-production.yml (on version tags)
   ├── Pre-deployment Checks
   ├── Security Audit
   ├── Build Production Artifacts
   ├── Database Backup
   ├── Blue-Green Deployment
   ├── Health Checks
   ├── Smoke Tests
   └── Monitoring Period
        │
        ▼
5. rollback.yml (manual trigger)
   ├── Backup Current State
   ├── Execute Rollback
   ├── Verify Health
   └── Notify Team
```

---

## Prerequisites

### Required Accounts and Access

1. **GitHub Repository**
   - Admin access to repository settings
   - Ability to create secrets and environments

2. **Cloud Provider**
   - AWS account with EKS/ECS access
   - OR GCP account with GKE access
   - IAM credentials with deployment permissions

3. **Container Registry**
   - GitHub Container Registry (ghcr.io) access
   - OR private Docker registry

4. **External Services**
   - SonarQube account (optional, for code quality)
   - Snyk account (optional, for security scanning)
   - Slack workspace (optional, for notifications)

---

## GitHub Secrets Configuration

### Navigate to Repository Secrets

```
GitHub Repository → Settings → Secrets and variables → Actions
```

### Required Secrets

#### Cloud Provider Credentials (AWS)

```yaml
AWS_ACCESS_KEY_ID: AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION: us-east-1
EKS_CLUSTER_NAME: motel-booking-cluster
```

#### Cloud Provider Credentials (GCP)

```yaml
GCP_SA_KEY: |
  {
    "type": "service_account",
    "project_id": "your-project",
    "private_key_id": "...",
    "private_key": "...",
    "client_email": "...",
    "client_id": "...",
    "auth_uri": "...",
    "token_uri": "...",
    "auth_provider_x509_cert_url": "...",
    "client_x509_cert_url": "..."
  }
GCP_REGION: us-central1
GKE_CLUSTER_NAME: motel-booking-cluster
```

#### Database Credentials

```yaml
# Staging Database
STAGING_DB_URL: jdbc:postgresql://staging-db.example.com:5432/motel_booking
STAGING_DB_USER: moteluser
STAGING_DB_PASSWORD: [secure-password]

# Production Database
PROD_DB_URL: jdbc:postgresql://prod-db.example.com:5432/motel_booking
PROD_DB_USER: moteluser
PROD_DB_PASSWORD: [secure-password]
```

#### Application Secrets

```yaml
JWT_SECRET: [64-character-random-string]
MFA_SECRET_KEY: [32-character-random-string]
ENCRYPTION_KEY: [32-character-random-string]
```

#### Email Configuration

```yaml
SMTP_HOST: smtp.gmail.com
SMTP_PORT: 587
SMTP_USERNAME: noreply@westbethelmotel.com
SMTP_PASSWORD: [app-specific-password]
SMTP_FROM: noreply@westbethelmotel.com
```

#### External Service Tokens

```yaml
SONAR_TOKEN: [sonarqube-token]
SONAR_HOST_URL: https://sonarcloud.io
SNYK_TOKEN: [snyk-api-token]
GITHUB_TOKEN: [automatically-provided]
```

#### Notification Services

```yaml
SLACK_WEBHOOK_URL: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
ONCALL_EMAIL: oncall@westbethelmotel.com
```

### Repository Variables

```yaml
CLOUD_PROVIDER: aws  # or 'gcp'
ENABLE_WAF: true
ENABLE_IMAGE_SIGNING: false
```

---

## Pipeline Workflows

### 1. Continuous Integration (ci.yml)

**Triggers:**
- Push to any branch
- Pull request to main/develop

**Steps:**
1. Code quality analysis (Checkstyle, SpotBugs, SonarQube)
2. Unit tests with coverage reporting
3. Integration tests with PostgreSQL and Redis
4. Security vulnerability scanning
5. Docker image build and push
6. Container vulnerability scanning

**Configuration:**
```yaml
# Enable/disable SonarQube
# Set SONAR_TOKEN and SONAR_HOST_URL secrets

# Enable/disable Snyk
# Set SNYK_TOKEN secret

# Configure test thresholds in pom.xml
```

### 2. Security Scanning (security-scan.yml)

**Triggers:**
- Daily schedule (2 AM UTC)
- Manual workflow dispatch
- Push to main/develop

**Scans:**
- OWASP Dependency Check
- Trivy container scanning
- Snyk security analysis
- CodeQL static analysis
- GitLeaks secret detection

**Automated Actions:**
- Creates GitHub issues for critical vulnerabilities
- Uploads SARIF results to GitHub Security tab
- Sends Slack notifications for failures

### 3. Staging Deployment (cd-staging.yml)

**Triggers:**
- Push to develop branch
- Manual workflow dispatch

**Process:**
1. Run full test suite
2. Build production-ready artifacts
3. Build and push Docker image
4. Run database migrations (with validation)
5. Deploy to Kubernetes staging namespace
6. Wait for rollout completion
7. Run smoke tests
8. Perform health checks
9. Send notifications

**Environment:** `staging`

### 4. Production Deployment (cd-production.yml)

**Triggers:**
- Push version tags (v*.*.*)
- Manual workflow dispatch

**Process:**
1. Pre-deployment validation
2. Security audit
3. Build production artifacts
4. Database backup
5. Database migration (dry-run then actual)
6. Blue-green deployment
7. Post-deployment health checks
8. Smoke tests
9. 1-hour monitoring period
10. Create GitHub release
11. Send notifications

**Environment:** `production`

**Protection Rules:**
- Requires manual approval
- Restricted to specific users
- Deployment branch restrictions

### 5. Emergency Rollback (rollback.yml)

**Triggers:**
- Manual workflow dispatch only

**Inputs:**
- Environment (staging/production)
- Target version (optional)
- Rollback reason (required)

**Process:**
1. Confirmation check
2. Backup current state
3. Database backup
4. Execute rollback
5. Verify deployment
6. Health and smoke tests
7. Send notifications
8. Create incident report

---

## Deployment Environments

### Configure GitHub Environments

**Navigate to:**
```
Repository → Settings → Environments
```

### Staging Environment

**Configuration:**
```yaml
Name: staging
Protection Rules:
  - None (auto-deploy on develop push)

Environment Secrets:
  - KUBE_CONFIG (base64 encoded)
  - DB_URL
  - DB_USER
  - DB_PASSWORD

Environment Variables:
  - ENVIRONMENT=staging
  - REPLICAS=2
```

### Production Environment

**Configuration:**
```yaml
Name: production
Protection Rules:
  - Required reviewers: [admin-users]
  - Wait timer: 0 minutes
  - Deployment branches: tags matching v*.*.*

Environment Secrets:
  - KUBE_CONFIG (base64 encoded)
  - DB_URL
  - DB_USER
  - DB_PASSWORD

Environment Variables:
  - ENVIRONMENT=production
  - REPLICAS=3
```

---

## Setting Up the Pipeline

### Step 1: Fork or Clone Repository

```bash
git clone https://github.com/your-org/west-bethel-motel-booking-system.git
cd west-bethel-motel-booking-system
```

### Step 2: Configure Secrets

1. Go to repository settings
2. Navigate to Secrets and variables → Actions
3. Add all required secrets listed above

### Step 3: Configure Environments

1. Go to repository settings
2. Navigate to Environments
3. Create `staging` and `production` environments
4. Configure protection rules
5. Add environment-specific secrets

### Step 4: Verify Workflows

```bash
# Check workflow syntax
act -l  # if using act for local testing

# Or push to a test branch
git checkout -b test/ci-pipeline
git push origin test/ci-pipeline
```

### Step 5: Test Staging Deployment

```bash
# Merge to develop to trigger staging deployment
git checkout develop
git merge feature/your-feature
git push origin develop
```

### Step 6: Test Production Deployment

```bash
# Create and push a version tag
git checkout main
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

---

## Workflow Customization

### Modify Deployment Strategy

Edit `.github/workflows/cd-production.yml`:

```yaml
# Change from blue-green to rolling update
- name: Deploy with rolling update
  run: |
    kubectl set image deployment/motel-booking-app \
      motel-booking-app=${{ env.IMAGE }} \
      -n production
    kubectl rollout status deployment/motel-booking-app -n production
```

### Add Custom Tests

Create `.github/workflows/custom-tests.yml`:

```yaml
name: Custom Tests
on: [push, pull_request]
jobs:
  custom-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run custom tests
        run: ./scripts/custom-tests.sh
```

### Configure Notifications

Edit notification steps in workflows:

```yaml
- name: Send Slack notification
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Deployment completed!",
        "channel": "#deployments"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Monitoring and Logs

### View Workflow Runs

```
GitHub Repository → Actions → Select Workflow → Select Run
```

### Download Artifacts

Artifacts are available for 90 days:
- Test reports
- Security scan results
- Build logs
- Deployment manifests

### View Logs

Click on any job in a workflow run to view detailed logs.

---

## Troubleshooting

### Common Issues

#### 1. Secrets Not Found

**Error:**
```
Error: Input required and not supplied: aws-access-key-id
```

**Solution:**
- Verify secret name matches exactly (case-sensitive)
- Check secret is in correct scope (repository vs environment)
- Ensure environment name matches in workflow

#### 2. Docker Build Failures

**Error:**
```
ERROR: failed to solve: failed to fetch oauth token
```

**Solution:**
- Verify GITHUB_TOKEN permissions
- Check Container Registry access
- Ensure Docker login step completed successfully

#### 3. Kubernetes Deployment Failures

**Error:**
```
Error from server (Forbidden): deployments.apps is forbidden
```

**Solution:**
- Verify KUBECONFIG is correctly configured
- Check service account permissions
- Ensure namespace exists

#### 4. Database Migration Failures

**Error:**
```
ERROR: relation "users" already exists
```

**Solution:**
- Check Flyway baseline configuration
- Verify migration scripts
- Review database state

### Debug Mode

Enable debug logging in workflow:

```yaml
env:
  ACTIONS_STEP_DEBUG: true
  ACTIONS_RUNNER_DEBUG: true
```

---

## Best Practices

1. **Always test in staging first**
2. **Use protected branches for main/develop**
3. **Require code reviews before merging**
4. **Keep secrets updated and rotated**
5. **Monitor workflow run times**
6. **Review and update workflows regularly**
7. **Document custom configurations**
8. **Use workflow dispatch for manual control**

---

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Kubernetes Deployment Best Practices](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Security Scanning Tools](https://owasp.org/www-community/Source_Code_Analysis_Tools)

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0
