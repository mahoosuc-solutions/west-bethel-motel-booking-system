# Emergency Rollback Guide

## Overview

This guide provides step-by-step procedures for emergency rollbacks of the West Bethel Motel Booking System.

⚠️ **CRITICAL:** This guide is for emergency use only. Always attempt to fix forward first.

---

## When to Rollback

### Rollback Criteria

Execute a rollback if:
- ✅ Critical production bug affecting users
- ✅ Data corruption or data loss detected
- ✅ Security vulnerability introduced
- ✅ Performance degradation > 50%
- ✅ Service availability < 95%
- ✅ Failed health checks after 10 minutes

### Do NOT Rollback For

- ❌ Minor UI issues
- ❌ Non-critical bugs with workarounds
- ❌ Features not working as expected (but not breaking)
- ❌ Missing features (not broken features)

---

## Rollback Decision Tree

```
┌─────────────────────────────────┐
│   Production Issue Detected     │
└────────────┬────────────────────┘
             │
             ▼
┌────────────────────────────────┐
│  Is it Critical?               │
│  (Affecting users/revenue)     │
└────┬───────────────────┬───────┘
     │Yes                │No
     ▼                   ▼
┌─────────────┐    ┌──────────────┐
│ Can Fix     │    │  Log Issue   │
│ Forward in  │    │  Schedule    │
│ < 30 min?   │    │  Fix         │
└─────┬──┬────┘    └──────────────┘
      │  │
  Yes │  │ No
      │  │
      ▼  ▼
 ┌────────┐  ┌──────────────┐
 │  Fix   │  │   ROLLBACK   │
 │Forward │  │              │
 └────────┘  └──────────────┘
```

---

## Pre-Rollback Checklist

### 1. Verify the Issue

```bash
# Check application health
./scripts/health-check.sh https://www.westbethelmotel.com

# Review recent logs
kubectl logs -l app=motel-booking-app -n production --tail=500

# Check error rates
kubectl top pods -n production

# Review monitoring dashboards
# - Grafana: Check error rates, latency
# - CloudWatch/Stackdriver: Check alarms
```

### 2. Identify Cause

```bash
# Get recent deployments
kubectl rollout history deployment/motel-booking-app -n production

# Check recent changes
git log --oneline -10

# Review deployment logs
kubectl logs -l app=motel-booking-app -n production --since=1h
```

### 3. Notify Stakeholders

```bash
# Alert channels:
# - Slack: #incidents
# - Email: oncall@westbethelmotel.com
# - Status Page: Update with "Investigating"

# Message template:
"INCIDENT: Production issue detected at [TIME]
Issue: [BRIEF DESCRIPTION]
Impact: [USER IMPACT]
Action: Preparing for potential rollback
ETA: 15 minutes"
```

---

## Rollback Procedures

### Method 1: Automated Rollback Script (Recommended)

```bash
# Navigate to project root
cd /path/to/west-bethel-motel-booking-system

# Execute rollback
./scripts/rollback.sh \
  --environment production \
  --reason "Critical bug causing data corruption in v1.2.0" \
  [--version v1.1.0]  # Optional: specific version

# The script will:
# 1. Confirm rollback decision
# 2. Backup current state
# 3. Backup database
# 4. Execute rollback
# 5. Verify health
# 6. Run smoke tests
```

### Method 2: Manual Kubernetes Rollback

#### Step 1: Backup Current State

```bash
# Backup current deployment
kubectl get deployment motel-booking-app -n production -o yaml \
  > rollback-backup-$(date +%Y%m%d-%H%M%S).yaml

# Backup database
./scripts/backup-database.sh production emergency-$(date +%Y%m%d-%H%M%S)
```

#### Step 2: Execute Rollback

```bash
# Option A: Rollback to previous version
kubectl rollout undo deployment/motel-booking-app -n production

# Option B: Rollback to specific version
kubectl set image deployment/motel-booking-app \
  motel-booking-app=ghcr.io/westbethel/motel-booking-system:v1.1.0 \
  -n production

# Wait for rollback completion
kubectl rollout status deployment/motel-booking-app -n production --timeout=10m
```

#### Step 3: Verify Rollback

```bash
# Check pods are running
kubectl get pods -n production -l app=motel-booking-app

# Check deployment
kubectl describe deployment motel-booking-app -n production

# Verify image version
kubectl get deployment motel-booking-app -n production \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
```

### Method 3: GitHub Actions Rollback Workflow

1. Navigate to GitHub Actions
2. Select "Emergency Rollback" workflow
3. Click "Run workflow"
4. Fill in parameters:
   - Environment: `production`
   - Target Version: `v1.1.0` (or leave empty for previous)
   - Reason: Describe the issue
5. Click "Run workflow"
6. Monitor workflow progress

---

## Post-Rollback Verification

### 1. Health Checks (Critical - Do Immediately)

```bash
# Run comprehensive health check
./scripts/health-check.sh https://www.westbethelmotel.com

# Expected output:
# [PASS] Actuator health check: UP
# [PASS] Readiness check: UP
# [PASS] Liveness check: UP
# [PASS] Database connectivity: UP
# [PASS] Redis connectivity: UP
```

### 2. Smoke Tests (Critical - Do Within 5 Minutes)

```bash
# Run smoke tests
./scripts/smoke-test.sh https://www.westbethelmotel.com

# Expected: All critical tests pass
# [PASS] Application is available
# [PASS] Health endpoint returns UP
# [PASS] Database connection is healthy
# [PASS] API authentication endpoint is working
# [PASS] Rooms API endpoint is accessible
```

### 3. Functional Verification (Do Within 15 Minutes)

```bash
# Test critical user flows:
# 1. User login
curl -X POST https://www.westbethelmotel.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# 2. View rooms
curl https://www.westbethelmotel.com/api/rooms

# 3. Create booking (if appropriate)
# Manual testing in browser recommended
```

### 4. Monitor for Stability (First Hour Critical)

```bash
# Watch logs for errors
kubectl logs -f -l app=motel-booking-app -n production

# Monitor metrics
# - Response times
# - Error rates
# - Resource usage

# Check every 5 minutes for first hour
for i in {1..12}; do
  echo "Stability check $i/12..."
  curl -f https://www.westbethelmotel.com/actuator/health || echo "FAILED"
  sleep 300
done
```

---

## Database Rollback

### If Database Migration Needs Rollback

⚠️ **EXTREME CAUTION:** Database rollbacks can cause data loss!

#### Step 1: Stop Application

```bash
# Scale down to prevent data corruption
kubectl scale deployment/motel-booking-app --replicas=0 -n production
```

#### Step 2: Restore Database

```bash
# List available backups
ls -lh backups/production/

# Restore from backup
./scripts/restore.sh production backups/production/backup-20241023-120000.sql.gz

# This will:
# 1. Confirm restore (requires typing 'RESTORE PRODUCTION')
# 2. Create safety backup of current state
# 3. Drop and recreate database
# 4. Restore from backup file
# 5. Verify restoration
```

#### Step 3: Restart Application

```bash
# Scale back up
kubectl scale deployment/motel-booking-app --replicas=3 -n production

# Verify
kubectl rollout status deployment/motel-booking-app -n production
```

---

## Communication Plan

### During Rollback

**Update status page:**
```
Status: Major Outage → Identified → Monitoring

Update:
[12:00] Issue detected - investigating
[12:05] Rollback initiated to previous stable version
[12:15] Rollback complete - monitoring system stability
[12:30] System stable - issue resolved
```

**Notify stakeholders:**
```
TO: team@westbethelmotel.com, oncall@westbethelmotel.com
SUBJECT: RESOLVED: Production Rollback Executed

Production rollback successfully completed at [TIME].

DETAILS:
- Version rolled back: v1.2.0 → v1.1.0
- Reason: [ISSUE DESCRIPTION]
- Duration: [DURATION]
- Impact: [USER IMPACT]
- Current Status: System stable and operational

NEXT STEPS:
- Post-mortem scheduled for [DATE/TIME]
- Fix planned for [TIMELINE]
```

### Post-Rollback

Create incident report (see template below).

---

## Incident Report Template

```markdown
# Incident Report: Production Rollback [DATE]

## Summary
- **Date:** 2024-10-23
- **Time:** 12:00 PM - 12:30 PM EST
- **Duration:** 30 minutes
- **Severity:** SEV-1 (Critical)
- **Status:** Resolved

## Timeline
- 12:00 - Issue detected by monitoring alerts
- 12:03 - Incident response team assembled
- 12:05 - Decision made to rollback
- 12:06 - Rollback initiated
- 12:15 - Rollback completed
- 12:20 - Health checks passed
- 12:30 - Confirmed stable, incident resolved

## Impact
- **Users Affected:** Approximately 500 users
- **Services Affected:** Booking creation, payment processing
- **Revenue Impact:** Estimated $XXX in lost bookings

## Root Cause
[Detailed explanation of what went wrong]

## Resolution
Rolled back from v1.2.0 to v1.1.0 which restored service functionality.

## Action Items
- [ ] Fix bug in v1.2.0 code
- [ ] Add automated test to catch this issue
- [ ] Review deployment process
- [ ] Update runbooks
- [ ] Conduct post-mortem meeting

## Lessons Learned
1. [What went well]
2. [What could be improved]
3. [Prevention measures for future]
```

---

## Rollback Testing

### Test Rollback in Staging

```bash
# Deploy to staging
./scripts/deploy.sh --environment staging --tag v1.2.0

# Simulate issue (if possible)
# ...

# Test rollback
./scripts/rollback.sh --environment staging --version v1.1.0

# Verify rollback works as expected
./scripts/health-check.sh https://staging.westbethelmotel.com
```

---

## Prevention Strategies

### Before Deployment

1. **Staging Testing:** All changes tested in staging
2. **Canary Deployments:** Gradual rollout to production
3. **Feature Flags:** Ability to disable features without deployment
4. **Database Migrations:** Always backward compatible
5. **Automated Tests:** Comprehensive test coverage

### Monitoring

1. **Health Checks:** Continuous monitoring
2. **Alerts:** Immediate notification of issues
3. **Dashboards:** Real-time visibility
4. **Logs:** Centralized logging for debugging

---

## Emergency Contacts

```
On-Call Engineer: oncall@westbethelmotel.com
Engineering Manager: manager@westbethelmotel.com
CTO: cto@westbethelmotel.com

Slack Channels:
- #incidents (primary)
- #engineering (notifications)
- #ops (coordination)

PagerDuty: https://westbethelmotel.pagerduty.com
```

---

## Quick Reference

```bash
# Health Check
./scripts/health-check.sh https://www.westbethelmotel.com

# Rollback (Automated)
./scripts/rollback.sh --environment production --reason "Bug description"

# Rollback (Manual)
kubectl rollout undo deployment/motel-booking-app -n production

# Database Restore
./scripts/restore.sh production BACKUP_FILE

# View Logs
kubectl logs -f -l app=motel-booking-app -n production

# Scale Down
kubectl scale deployment/motel-booking-app --replicas=0 -n production

# Scale Up
kubectl scale deployment/motel-booking-app --replicas=3 -n production
```

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0

⚠️ **KEEP THIS GUIDE ACCESSIBLE AT ALL TIMES**
