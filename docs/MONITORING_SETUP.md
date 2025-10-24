# Monitoring Setup Guide

## Overview

This guide covers the setup and configuration of comprehensive monitoring for the West Bethel Motel Booking System using Prometheus, Grafana, and Zipkin for distributed tracing.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Prometheus Setup](#prometheus-setup)
- [Grafana Setup](#grafana-setup)
- [Zipkin Setup](#zipkin-setup)
- [Alert Configuration](#alert-configuration)
- [Troubleshooting](#troubleshooting)

## Prerequisites

- Docker and Docker Compose installed
- Application running with actuator endpoints enabled
- Access to `/actuator/prometheus` endpoint

## Prometheus Setup

### 1. Install Prometheus

Using Docker:

```bash
docker run -d \
  --name prometheus \
  -p 9090:9090 \
  -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v /path/to/prometheus-alerts.yml:/etc/prometheus/alerts.yml \
  prom/prometheus:latest
```

### 2. Configure Prometheus

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'motel-booking-prod'
    environment: 'production'

# Load alert rules
rule_files:
  - 'alerts.yml'

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

# Scrape configurations
scrape_configs:
  # Motel Booking System
  - job_name: 'motel-booking-system'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'motel-booking-system'

  # PostgreSQL Exporter
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### 3. Copy Alert Rules

Copy the provided `prometheus-alerts.yml` to your Prometheus configuration directory:

```bash
cp prometheus-alerts.yml /etc/prometheus/alerts.yml
```

### 4. Verify Prometheus

Access Prometheus UI at `http://localhost:9090` and verify:
- Targets are up (Status > Targets)
- Alerts are loaded (Alerts)
- Metrics are being collected

## Grafana Setup

### 1. Install Grafana

Using Docker:

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -e "GF_SECURITY_ADMIN_PASSWORD=admin" \
  grafana/grafana:latest
```

### 2. Add Prometheus Data Source

1. Access Grafana at `http://localhost:3000`
2. Login (default: admin/admin)
3. Go to Configuration > Data Sources
4. Add Prometheus data source:
   - URL: `http://prometheus:9090`
   - Access: Server (default)
   - Save & Test

### 3. Import Dashboard

Create a new dashboard or use the provided template:

```json
{
  "dashboard": {
    "title": "West Bethel Motel - System Overview",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_total[5m])"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_total{status=~\"5..\"}[5m])"
          }
        ]
      },
      {
        "title": "Response Time (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}"
          }
        ]
      }
    ]
  }
}
```

### 4. Create Business Metrics Dashboard

Add panels for:
- Total Bookings Today
- Revenue Today
- Payment Success Rate
- Active User Sessions
- Cache Hit Ratio
- Email Queue Size

### 5. Setup Alerting

Configure Grafana alerts for critical metrics:
- Error rate > 5%
- Response time > 1s
- Memory usage > 80%

## Zipkin Setup

### 1. Install Zipkin

Using Docker:

```bash
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  openzipkin/zipkin:latest
```

### 2. Configure Application

The application is already configured to send traces to Zipkin via the `application.yml`:

```yaml
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (reduce in production)
```

### 3. View Traces

Access Zipkin UI at `http://localhost:9411` to:
- Search for traces
- View service dependencies
- Analyze request flows
- Identify performance bottlenecks

## Alert Configuration

### Alertmanager Setup

1. Install Alertmanager:

```bash
docker run -d \
  --name alertmanager \
  -p 9093:9093 \
  -v /path/to/alertmanager.yml:/etc/alertmanager/alertmanager.yml \
  prom/alertmanager:latest
```

2. Configure `alertmanager.yml`:

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@westbethelmotel.com'
  smtp_auth_username: 'your-email@gmail.com'
  smtp_auth_password: 'your-app-password'

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'admin-email'

  routes:
    - match:
        severity: critical
      receiver: 'admin-email'
      continue: true

    - match:
        severity: warning
      receiver: 'admin-email'

receivers:
  - name: 'admin-email'
    email_configs:
      - to: 'admin@westbethelmotel.com'
        headers:
          Subject: '[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
```

## Docker Compose Setup

Complete monitoring stack with Docker Compose:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus-alerts.yml:/etc/prometheus/alerts.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana

  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"

  alertmanager:
    image: prom/alertmanager:latest
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml

  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    environment:
      - DATA_SOURCE_NAME=postgresql://user:password@postgres:5432/database?sslmode=disable
    ports:
      - "9187:9187"

  redis-exporter:
    image: oliver006/redis_exporter:latest
    environment:
      - REDIS_ADDR=redis:6379
    ports:
      - "9121:9121"

volumes:
  prometheus-data:
  grafana-data:
```

Start the stack:

```bash
docker-compose up -d
```

## Health Checks

The application exposes the following health check endpoints:

- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe
- `/actuator/health/db` - Database health
- `/actuator/health/redis` - Redis health
- `/actuator/health/mail` - Email service health

## Metrics Endpoints

- `/actuator/metrics` - List of available metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

## Troubleshooting

### Metrics Not Appearing in Prometheus

1. Check if actuator endpoints are exposed:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Verify Prometheus scrape configuration
3. Check Prometheus logs for errors

### High Memory Usage Alerts

1. Check heap dump:
   ```bash
   curl http://localhost:8080/actuator/heapdump > heapdump.hprof
   ```

2. Analyze with tools like VisualVM or Eclipse MAT

### Zipkin Not Receiving Traces

1. Verify Zipkin endpoint configuration
2. Check application logs for tracing errors
3. Ensure network connectivity to Zipkin

### Database Connection Pool Issues

1. Check active connections metric:
   ```
   database_connections_active
   ```

2. Review HikariCP configuration in `application.yml`
3. Analyze slow query logs

## Production Recommendations

1. **Sampling Rate**: Reduce tracing sampling to 10-20% in production
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.1
   ```

2. **Retention**: Configure appropriate data retention periods
   - Prometheus: 15-30 days
   - Zipkin: 7 days
   - Logs: 30-90 days

3. **High Availability**: Run multiple instances of monitoring components

4. **Security**:
   - Enable authentication on Prometheus/Grafana
   - Use TLS for all connections
   - Restrict network access

5. **Alerting**: Configure multiple notification channels (email, Slack, PagerDuty)

## Support

For issues or questions:
- Check application logs in `logs/` directory
- Review Prometheus alerts
- Consult the metrics reference guide

## See Also

- [METRICS_REFERENCE.md](./METRICS_REFERENCE.md) - Complete metrics documentation
- [Application Configuration](../src/main/resources/application.yml)
- [Prometheus Alert Rules](../prometheus-alerts.yml)
