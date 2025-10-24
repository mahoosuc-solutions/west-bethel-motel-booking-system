# Monitoring and Operations Runbook

## Overview

This runbook provides operational procedures and troubleshooting guidance for monitoring the West Bethel Motel Booking System in production.

---

## Table of Contents

1. [Monitoring Stack](#monitoring-stack)
2. [Key Metrics](#key-metrics)
3. [Alert Response Procedures](#alert-response-procedures)
4. [Common Issues](#common-issues)
5. [Runbook Procedures](#runbook-procedures)
6. [Emergency Contacts](#emergency-contacts)

---

## Monitoring Stack

### Components

```
┌─────────────────────────────────────────┐
│          Monitoring Stack               │
├─────────────────────────────────────────┤
│  Prometheus  │  Metrics Collection      │
│  Grafana     │  Visualization           │
│  AlertManager│  Alert Routing           │
│  CloudWatch  │  AWS Metrics & Logs      │
│  ELK Stack   │  Log Aggregation         │
└─────────────────────────────────────────┘
```

### Access URLs

```
Grafana:        https://grafana.westbethelmotel.com
Prometheus:     https://prometheus.westbethelmotel.com
Kibana:         https://kibana.westbethelmotel.com
CloudWatch:     AWS Console → CloudWatch
```

---

## Key Metrics

### Application Metrics

#### Response Time
- **Metric:** `http_server_requests_seconds`
- **Normal:** < 200ms (p95)
- **Warning:** 200-500ms (p95)
- **Critical:** > 500ms (p95)

```promql
# Query
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
)
```

#### Error Rate
- **Metric:** `http_server_requests_total{status=~"5.."}`
- **Normal:** < 0.1%
- **Warning:** 0.1-1%
- **Critical:** > 1%

```promql
# Query
rate(http_server_requests_total{status=~"5.."}[5m])
/
rate(http_server_requests_total[5m]) * 100
```

#### Throughput
- **Metric:** `http_server_requests_total`
- **Normal:** Varies by time of day
- **Baseline:** 100-500 req/min

```promql
# Query
rate(http_server_requests_total[5m]) * 60
```

### Infrastructure Metrics

#### CPU Usage
- **Normal:** < 70%
- **Warning:** 70-85%
- **Critical:** > 85%

```bash
kubectl top pods -n production
```

#### Memory Usage
- **Normal:** < 80%
- **Warning:** 80-90%
- **Critical:** > 90%

#### Pod Health
- **Normal:** All pods running
- **Warning:** 1 pod down
- **Critical:** > 1 pod down

```bash
kubectl get pods -n production
```

### Database Metrics

#### Connection Pool
- **Normal:** < 80% utilized
- **Warning:** 80-95%
- **Critical:** > 95%

#### Query Performance
- **Normal:** < 100ms (average)
- **Warning:** 100-500ms
- **Critical:** > 500ms

#### Slow Queries
- **Threshold:** > 1 second
- **Action:** Log and investigate

### Cache Metrics

#### Redis Hit Rate
- **Normal:** > 90%
- **Warning:** 80-90%
- **Critical:** < 80%

```promql
# Query
rate(redis_keyspace_hits_total[5m])
/
(rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) * 100
```

---

## Alert Response Procedures

### SEV-1: Critical - Immediate Response Required

#### High Error Rate Alert

**Alert:** Error rate > 5% for 5 minutes

**Response:**
1. **Acknowledge alert** (< 2 minutes)
2. **Check service status**
   ```bash
   kubectl get pods -n production
   ./scripts/health-check.sh https://www.westbethelmotel.com
   ```
3. **Review recent changes**
   ```bash
   kubectl rollout history deployment/motel-booking-app -n production
   git log --oneline -10
   ```
4. **Check logs for errors**
   ```bash
   kubectl logs -l app=motel-booking-app -n production --tail=500 | grep ERROR
   ```
5. **Escalate if not resolved in 15 minutes**
6. **Consider rollback** if issue persists

#### Service Down Alert

**Alert:** All pods unhealthy

**Response:**
1. **Immediate escalation** to on-call engineer
2. **Check cluster health**
   ```bash
   kubectl get nodes
   kubectl get pods --all-namespaces
   ```
3. **Check deployment**
   ```bash
   kubectl describe deployment motel-booking-app -n production
   ```
4. **Review events**
   ```bash
   kubectl get events -n production --sort-by='.lastTimestamp'
   ```
5. **If deployment issue, rollback immediately**
   ```bash
   ./scripts/rollback.sh --environment production --reason "Service down"
   ```

#### Database Connection Failure

**Alert:** Database connectivity issues

**Response:**
1. **Check database status**
   ```bash
   # AWS RDS
   aws rds describe-db-instances --db-instance-identifier motel-booking-db

   # Or via kubectl
   kubectl exec -it POD_NAME -n production -- psql -h DB_HOST -U DB_USER -c "SELECT 1"
   ```
2. **Check connection pool**
   ```bash
   # View metrics in Grafana
   # Check "Database Connections" dashboard
   ```
3. **Check security groups/firewall rules**
4. **Restart pods if connections are exhausted**
   ```bash
   kubectl rollout restart deployment/motel-booking-app -n production
   ```
5. **Scale up RDS if needed** (for performance issues)

### SEV-2: High Priority - Response Within 30 Minutes

#### High Response Time Alert

**Alert:** P95 response time > 1 second

**Response:**
1. **Check current load**
   ```bash
   kubectl top pods -n production
   ```
2. **Review slow queries**
   ```sql
   SELECT pid, now() - query_start as duration, query
   FROM pg_stat_activity
   WHERE state = 'active'
   ORDER BY duration DESC;
   ```
3. **Check cache hit rate**
4. **Consider scaling up**
   ```bash
   kubectl scale deployment/motel-booking-app --replicas=5 -n production
   ```
5. **Investigate N+1 queries or inefficient code**

#### High Memory Usage Alert

**Alert:** Memory usage > 90%

**Response:**
1. **Check for memory leaks**
   ```bash
   kubectl top pods -n production
   kubectl describe pod POD_NAME -n production
   ```
2. **Review heap dumps** (if available)
3. **Restart affected pods**
   ```bash
   kubectl delete pod POD_NAME -n production
   ```
4. **Consider increasing memory limits** if legitimate usage

### SEV-3: Medium Priority - Response Within 2 Hours

#### Low Cache Hit Rate

**Response:**
1. Check cache configuration
2. Review cache eviction policies
3. Analyze cache key patterns
4. Consider increasing cache size

#### Disk Space Warning

**Response:**
1. Check disk usage
2. Clean old logs
3. Implement log rotation
4. Increase disk size if needed

---

## Common Issues

### Issue: Pods in CrashLoopBackOff

**Symptoms:**
```bash
$ kubectl get pods -n production
NAME                                 READY   STATUS             RESTARTS
motel-booking-app-5d7c4d8f6b-xyz12  0/1     CrashLoopBackOff   5
```

**Diagnosis:**
```bash
# Check pod logs
kubectl logs POD_NAME -n production --previous

# Common causes:
# - Application startup failure
# - Missing environment variables
# - Database connection failure
# - Out of memory
```

**Resolution:**
```bash
# Fix configuration
kubectl edit deployment motel-booking-app -n production

# Or rollback
kubectl rollout undo deployment/motel-booking-app -n production
```

### Issue: ImagePullBackOff

**Symptoms:**
```
Events:
  Failed to pull image: authentication required
```

**Resolution:**
```bash
# Check image exists
docker pull IMAGE_NAME:TAG

# Verify secret exists
kubectl get secret regcred -n production

# Recreate secret if needed
kubectl create secret docker-registry regcred \
  --docker-server=ghcr.io \
  --docker-username=USERNAME \
  --docker-password=TOKEN \
  -n production
```

### Issue: High Database CPU

**Symptoms:**
- Slow query performance
- RDS CPU > 80%

**Diagnosis:**
```sql
-- Find slow queries
SELECT pid, now() - query_start as duration, query, state
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC
LIMIT 10;

-- Check locks
SELECT * FROM pg_locks WHERE granted = false;
```

**Resolution:**
1. Terminate long-running queries
2. Add missing indexes
3. Optimize query patterns
4. Consider read replicas
5. Scale up RDS instance

### Issue: Redis Connection Timeout

**Symptoms:**
```
RedisConnectionFailureException: Unable to connect to Redis
```

**Resolution:**
```bash
# Check Redis health
kubectl exec -it POD_NAME -n production -- redis-cli -h REDIS_HOST ping

# Check network policies
kubectl get networkpolicies -n production

# Verify credentials
kubectl get secret redis-credentials -n production -o yaml

# Restart Redis if needed (staging only!)
```

---

## Runbook Procedures

### Procedure: Scale Application

```bash
# Manual scaling
kubectl scale deployment/motel-booking-app --replicas=N -n production

# Verify
kubectl get deployment motel-booking-app -n production

# HPA will override manual scaling
# To disable HPA temporarily:
kubectl delete hpa motel-booking-hpa -n production

# Remember to recreate HPA after maintenance
kubectl apply -f k8s/hpa.yaml
```

### Procedure: Restart Application

```bash
# Graceful restart (rolling)
kubectl rollout restart deployment/motel-booking-app -n production

# Force restart (delete pods)
kubectl delete pods -l app=motel-booking-app -n production

# Restart single pod
kubectl delete pod POD_NAME -n production
```

### Procedure: Update Environment Variables

```bash
# Edit deployment
kubectl edit deployment motel-booking-app -n production

# Or use configmap
kubectl edit configmap motel-booking-config -n production

# Restart to apply changes
kubectl rollout restart deployment/motel-booking-app -n production
```

### Procedure: Database Maintenance Window

```bash
# 1. Scale down application
kubectl scale deployment/motel-booking-app --replicas=0 -n production

# 2. Perform database maintenance
# (backup, vacuum, reindex, etc.)

# 3. Scale back up
kubectl scale deployment/motel-booking-app --replicas=3 -n production

# 4. Verify health
./scripts/health-check.sh https://www.westbethelmotel.com
```

### Procedure: Enable Debug Logging

```bash
# Temporarily enable debug logging
kubectl set env deployment/motel-booking-app \
  LOGGING_LEVEL_ROOT=DEBUG \
  -n production

# Revert after debugging
kubectl set env deployment/motel-booking-app \
  LOGGING_LEVEL_ROOT=INFO \
  -n production
```

---

## Dashboards

### Grafana Dashboards

1. **Application Overview**
   - Request rate, latency, errors
   - JVM metrics
   - Cache hit rate

2. **Infrastructure**
   - Pod CPU/Memory
   - Network I/O
   - Disk usage

3. **Database**
   - Connection pool
   - Query performance
   - Slow queries

4. **Business Metrics**
   - Booking rate
   - Revenue per hour
   - User registrations

---

## Emergency Contacts

```
Level 1 - On-Call Engineer
  - Slack: @oncall
  - Email: oncall@westbethelmotel.com
  - Phone: +1-XXX-XXX-XXXX

Level 2 - Engineering Manager
  - Slack: @eng-manager
  - Email: manager@westbethelmotel.com

Level 3 - CTO
  - Email: cto@westbethelmotel.com

Vendor Support:
  - AWS Support: https://console.aws.amazon.com/support
  - Database DBA: dba@westbethelmotel.com
```

---

## Escalation Matrix

| Severity | Response Time | Escalation After |
|----------|---------------|------------------|
| SEV-1    | 5 minutes     | 15 minutes       |
| SEV-2    | 30 minutes    | 2 hours          |
| SEV-3    | 2 hours       | 8 hours          |

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0
