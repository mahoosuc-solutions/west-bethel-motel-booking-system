# Metrics Reference Guide

## Overview

This document provides a comprehensive reference of all custom metrics exposed by the West Bethel Motel Booking System.

## Table of Contents

- [Business Metrics](#business-metrics)
- [Security Metrics](#security-metrics)
- [Performance Metrics](#performance-metrics)
- [Infrastructure Metrics](#infrastructure-metrics)
- [Notification Metrics](#notification-metrics)
- [Query Examples](#query-examples)
- [Alert Thresholds](#alert-thresholds)

## Business Metrics

### bookings.created

**Type**: Counter
**Description**: Total number of bookings created since application start
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# Bookings created per hour
rate(bookings_created_total[1h]) * 3600

# Total bookings today
increase(bookings_created_total[24h])
```

### bookings.cancelled

**Type**: Counter
**Description**: Total number of bookings cancelled
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# Cancellation rate
rate(bookings_cancelled_total[1h]) / rate(bookings_created_total[1h])

# Percentage of cancellations
(bookings_cancelled_total / bookings_created_total) * 100
```

### payments.success

**Type**: Counter
**Description**: Total number of successful payment transactions
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# Payment success rate
payments_success_total / (payments_success_total + payments_failure_total)

# Successful payments per hour
rate(payments_success_total[1h]) * 3600
```

### payments.failure

**Type**: Counter
**Description**: Total number of failed payment transactions
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# Payment failure rate
rate(payments_failure_total[5m]) / (rate(payments_success_total[5m]) + rate(payments_failure_total[5m]))
```

### revenue.total

**Type**: Gauge
**Description**: Total revenue in cents
**Tags**: `type=business`
**Unit**: cents

**Query Examples**:
```promql
# Revenue in dollars
revenue_total / 100

# Revenue per hour
rate(revenue_total[1h]) * 3600 / 100
```

### users.registered

**Type**: Counter
**Description**: Total number of user registrations
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# User registrations per day
increase(users_registered_total[24h])

# Registration rate per hour
rate(users_registered_total[1h]) * 3600
```

## Security Metrics

### auth.success

**Type**: Counter
**Description**: Total number of successful authentication attempts
**Tags**: `type=security`
**Unit**: count

**Query Examples**:
```promql
# Authentication success rate
auth_success_total / (auth_success_total + auth_failure_total)

# Successful logins per minute
rate(auth_success_total[1m]) * 60
```

### auth.failure

**Type**: Counter
**Description**: Total number of failed authentication attempts
**Tags**: `type=security`
**Unit**: count

**Alert Threshold**: Rate > 10/second for 5 minutes

**Query Examples**:
```promql
# Failed authentication rate
rate(auth_failure_total[5m])

# Authentication failure percentage
(auth_failure_total / (auth_success_total + auth_failure_total)) * 100
```

### jwt.validation.time

**Type**: Timer
**Description**: Time taken to validate JWT tokens
**Tags**: `type=security`
**Unit**: milliseconds

**Query Examples**:
```promql
# Average JWT validation time
rate(jwt_validation_time_seconds_sum[5m]) / rate(jwt_validation_time_seconds_count[5m]) * 1000

# 95th percentile validation time
histogram_quantile(0.95, rate(jwt_validation_time_seconds_bucket[5m])) * 1000
```

### sessions.active

**Type**: Gauge
**Description**: Number of currently active user sessions
**Tags**: `type=business`
**Unit**: count

**Query Examples**:
```promql
# Current active sessions
sessions_active

# Peak sessions in last hour
max_over_time(sessions_active[1h])
```

## Performance Metrics

### cache.hits

**Type**: Counter
**Description**: Total number of cache hits
**Tags**: `type=performance`
**Unit**: count

**Query Examples**:
```promql
# Cache hit ratio
cache_hits_total / (cache_hits_total + cache_misses_total)

# Cache hits per second
rate(cache_hits_total[5m])
```

### cache.misses

**Type**: Counter
**Description**: Total number of cache misses
**Tags**: `type=performance`
**Unit**: count

**Alert Threshold**: Hit ratio < 50% for 10 minutes

**Query Examples**:
```promql
# Cache miss rate
cache_misses_total / (cache_hits_total + cache_misses_total)

# Cache effectiveness
(cache_hits_total / (cache_hits_total + cache_misses_total)) * 100
```

### cache.operation.time

**Type**: Timer
**Description**: Time taken for cache operations (get/set)
**Tags**: `type=performance`
**Unit**: milliseconds

**Query Examples**:
```promql
# Average cache operation time
rate(cache_operation_time_seconds_sum[5m]) / rate(cache_operation_time_seconds_count[5m]) * 1000

# p99 cache operation time
histogram_quantile(0.99, rate(cache_operation_time_seconds_bucket[5m])) * 1000
```

### database.query.time

**Type**: Timer
**Description**: Time taken to execute database queries
**Tags**: `type=infrastructure`
**Unit**: milliseconds

**Alert Threshold**: p95 > 1000ms for 5 minutes

**Query Examples**:
```promql
# Average query time
rate(database_query_time_seconds_sum[5m]) / rate(database_query_time_seconds_count[5m]) * 1000

# Slow query detection (p95)
histogram_quantile(0.95, rate(database_query_time_seconds_bucket[5m])) * 1000
```

## Infrastructure Metrics

### database.connections.active

**Type**: Gauge
**Description**: Number of active database connections in the pool
**Tags**: `type=infrastructure`
**Unit**: count

**Alert Threshold**: > 80% of max pool size for 5 minutes

**Query Examples**:
```promql
# Connection pool usage percentage
(database_connections_active / 20) * 100

# Peak connections in last hour
max_over_time(database_connections_active[1h])
```

### email.queue.size

**Type**: Gauge
**Description**: Current size of the email queue
**Tags**: `type=notification`
**Unit**: count

**Alert Threshold**: > 1000 emails for 10 minutes

**Query Examples**:
```promql
# Current queue size
email_queue_size

# Queue growth rate
deriv(email_queue_size[5m])
```

## Notification Metrics

### emails.sent

**Type**: Counter
**Description**: Total number of emails sent successfully
**Tags**: `type=notification`
**Unit**: count

**Query Examples**:
```promql
# Emails sent per hour
rate(emails_sent_total[1h]) * 3600

# Email success rate
emails_sent_total / (emails_sent_total + emails_failed_total)
```

### emails.failed

**Type**: Counter
**Description**: Total number of failed email sends
**Tags**: `type=notification`
**Unit**: count

**Query Examples**:
```promql
# Email failure rate
rate(emails_failed_total[5m]) / (rate(emails_sent_total[5m]) + rate(emails_failed_total[5m]))
```

### email.send.time

**Type**: Timer
**Description**: Time taken to send emails
**Tags**: `type=notification`
**Unit**: milliseconds

**Query Examples**:
```promql
# Average email send time
rate(email_send_time_seconds_sum[5m]) / rate(email_send_time_seconds_count[5m]) * 1000

# p95 email send time
histogram_quantile(0.95, rate(email_send_time_seconds_bucket[5m])) * 1000
```

### payment.processing.time

**Type**: Timer
**Description**: Time taken to process payment transactions
**Tags**: `type=business`
**Unit**: milliseconds

**Query Examples**:
```promql
# Average payment processing time
rate(payment_processing_time_seconds_sum[5m]) / rate(payment_processing_time_seconds_count[5m]) * 1000

# p99 payment processing time
histogram_quantile(0.99, rate(payment_processing_time_seconds_bucket[5m])) * 1000
```

## Standard Spring Boot Metrics

### http.server.requests

**Type**: Timer
**Description**: HTTP request metrics
**Tags**: `uri`, `method`, `status`, `exception`
**Unit**: seconds

**Query Examples**:
```promql
# Request rate by endpoint
sum(rate(http_server_requests_total[5m])) by (uri)

# Error rate
rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m])

# p95 response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Requests per second
sum(rate(http_server_requests_total[1m]))
```

### jvm.memory.used

**Type**: Gauge
**Description**: JVM memory usage
**Tags**: `area` (heap/nonheap), `id`
**Unit**: bytes

**Alert Threshold**: heap > 80% of max for 5 minutes

**Query Examples**:
```promql
# Heap memory usage percentage
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# Memory usage in MB
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

### system.cpu.usage

**Type**: Gauge
**Description**: System CPU usage
**Unit**: percentage (0-1)

**Alert Threshold**: > 0.80 for 10 minutes

**Query Examples**:
```promql
# CPU usage percentage
system_cpu_usage * 100

# Average CPU over 5 minutes
avg_over_time(system_cpu_usage[5m]) * 100
```

## Query Examples

### Dashboard Queries

**Active Users**:
```promql
sessions_active
```

**Requests Per Second**:
```promql
sum(rate(http_server_requests_total[1m]))
```

**Error Rate**:
```promql
sum(rate(http_server_requests_total{status=~"5.."}[5m])) / sum(rate(http_server_requests_total[5m])) * 100
```

**Average Response Time**:
```promql
histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000
```

**Memory Usage**:
```promql
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100
```

**Database Connection Pool**:
```promql
database_connections_active
```

**Cache Hit Ratio**:
```promql
cache_hits_total / (cache_hits_total + cache_misses_total) * 100
```

**Revenue Today**:
```promql
increase(revenue_total[24h]) / 100
```

**Payment Success Rate**:
```promql
payments_success_total / (payments_success_total + payments_failure_total) * 100
```

## Alert Thresholds

### Critical Alerts

| Metric | Threshold | Duration | Severity |
|--------|-----------|----------|----------|
| Error Rate | > 5% | 5 minutes | Critical |
| Payment Failure Rate | > 10% | 10 minutes | Critical |
| Memory Usage | > 90% | 5 minutes | Critical |
| Database Connections | > 95% | 5 minutes | Critical |
| Authentication Failure Spike | > 10/sec | 5 minutes | Critical |

### Warning Alerts

| Metric | Threshold | Duration | Severity |
|--------|-----------|----------|----------|
| Response Time (p95) | > 1 second | 5 minutes | Warning |
| Memory Usage | > 80% | 5 minutes | Warning |
| Cache Miss Rate | > 50% | 10 minutes | Warning |
| Email Queue Size | > 1000 | 10 minutes | Warning |
| Database Connections | > 80% | 5 minutes | Warning |
| CPU Usage | > 80% | 10 minutes | Warning |

## Recording Rules

Create recording rules in Prometheus for frequently used queries:

```yaml
groups:
  - name: recording_rules
    interval: 30s
    rules:
      - record: job:http_requests:rate5m
        expr: sum(rate(http_server_requests_total[5m])) by (job)

      - record: job:http_errors:rate5m
        expr: sum(rate(http_server_requests_total{status=~"5.."}[5m])) by (job)

      - record: job:error_rate:ratio
        expr: job:http_errors:rate5m / job:http_requests:rate5m

      - record: job:cache_hit_ratio
        expr: cache_hits_total / (cache_hits_total + cache_misses_total)

      - record: job:payment_success_rate
        expr: payments_success_total / (payments_success_total + payments_failure_total)
```

## Best Practices

1. **Use rate() for counters**: Always use `rate()` or `increase()` when querying counter metrics
2. **Aggregate before calculation**: Use `sum()` before calculating ratios
3. **Use recording rules**: Pre-calculate expensive queries
4. **Set appropriate time ranges**: Use 5m for most queries, 1h for trends
5. **Label carefully**: Avoid high cardinality labels
6. **Use quantiles for timers**: p50, p95, p99 for latency metrics

## Integration with Grafana

Import these queries into Grafana dashboards for real-time monitoring. Use variables for dynamic filtering:

- `$environment`: Environment filter
- `$instance`: Instance filter
- `$interval`: Time range interval

## See Also

- [MONITORING_SETUP.md](./MONITORING_SETUP.md) - Setup instructions
- [Application Configuration](../src/main/resources/application.yml)
- [Prometheus Alert Rules](../prometheus-alerts.yml)
