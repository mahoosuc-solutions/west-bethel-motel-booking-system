# Monitoring Quick Start Guide
## West Bethel Motel Booking System

### For Developers: How to Use Metrics in Your Code

#### 1. Inject MetricsService

```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final MetricsService metricsService;

    public void yourMethod() {
        // Your business logic
    }
}
```

#### 2. Record Business Events

```java
// Record a booking
metricsService.recordBookingCreated(booking.getTotalAmount());

// Record a cancellation
metricsService.recordBookingCancelled();

// Record payment
metricsService.recordPaymentSuccess(amount);
metricsService.recordPaymentFailure("Card declined");

// Record user registration
metricsService.recordUserRegistration();

// Record authentication
metricsService.recordAuthenticationSuccess(username);
metricsService.recordAuthenticationFailure(username, reason);
```

#### 3. Record Cache Events

```java
// In your cache service
metricsService.recordCacheHit("userCache");
metricsService.recordCacheMiss("userCache");
```

#### 4. Record Email Events

```java
// After sending email
metricsService.recordEmailSent(recipient);

// On failure
metricsService.recordEmailFailed(recipient, reason);

// Update queue size
metricsService.updateEmailQueueSize(queueSize);
```

#### 5. Time Operations

```java
// Time a database query
long startTime = System.currentTimeMillis();
try {
    // Your query
} finally {
    long duration = System.currentTimeMillis() - startTime;
    metricsService.recordDatabaseQueryTime(duration);
}

// Or use the timer utility
String result = metricsService.timeOperation(
    businessMetrics.getPaymentProcessingTimer(),
    () -> processPayment()
);
```

#### 6. Update Session Count

```java
// When session created
metricsService.incrementActiveUserSessions();

// When session destroyed
metricsService.decrementActiveUserSessions();

// Or set directly
metricsService.updateActiveUserSessions(count);
```

---

### Quick Access URLs

#### Local Development
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Health Check**: http://localhost:8080/actuator/health
- **All Metrics**: http://localhost:8080/actuator/metrics

#### Monitoring Stack (if deployed)
- **Prometheus UI**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Zipkin**: http://localhost:9411

#### Admin Dashboard API
- **Metrics**: GET http://localhost:8080/api/v1/admin/dashboard/metrics
- **Health**: GET http://localhost:8080/api/v1/admin/dashboard/system-health
- **Activity**: GET http://localhost:8080/api/v1/admin/dashboard/recent-activity

---

### Common Prometheus Queries

```promql
# Request rate
rate(http_server_requests_total[5m])

# Error rate
rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m])

# Response time (p95)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Bookings today
increase(bookings_created_total[24h])

# Payment success rate
payments_success_total / (payments_success_total + payments_failure_total)

# Cache hit ratio
cache_hits_total / (cache_hits_total + cache_misses_total)

# Active sessions
sessions_active

# Memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

---

### Testing Metrics

#### Unit Tests
```java
@Test
void shouldRecordMetric() {
    // Given
    metricsService.recordBookingCreated(BigDecimal.valueOf(100));

    // Then
    var counter = meterRegistry.find("bookings.created").counter();
    assertThat(counter.count()).isEqualTo(1.0);
}
```

#### Manual Testing
```bash
# Create a booking and check metrics
curl http://localhost:8080/actuator/metrics/bookings.created

# View all available metrics
curl http://localhost:8080/actuator/metrics

# Check Prometheus format
curl http://localhost:8080/actuator/prometheus | grep bookings
```

---

### Troubleshooting

#### Metrics Not Appearing
1. Check if actuator endpoints are exposed in `application.yml`
2. Verify Prometheus dependency in `pom.xml`
3. Ensure `@EnableScheduling` is on main application class
4. Check logs for initialization errors

#### Alerts Not Triggering
1. Verify alert thresholds in `application.yml`
2. Check `AlertingService` is scheduled correctly
3. Review logs for alert conditions
4. Ensure metrics are being recorded

#### Health Checks Failing
1. Check database connectivity
2. Verify Redis is running
3. Test SMTP server connection
4. Review component-specific logs

---

### Environment Variables for Production

```bash
# Alert Thresholds
MONITORING_ALERTS_ENABLED=true
ALERT_ERROR_RATE_THRESHOLD=0.05
ALERT_RESPONSE_TIME_THRESHOLD=1000
ALERT_MEMORY_THRESHOLD=0.80

# Tracing
TRACING_SAMPLING_PROBABILITY=0.1  # 10% in production
ZIPKIN_URL=http://zipkin:9411/api/v2/spans
```

---

### Best Practices

1. **Record Early**: Add metrics when implementing new features
2. **Use Timers**: Always time operations that might be slow
3. **Add Context**: Use descriptive metric names and tags
4. **Test Metrics**: Write unit tests for metric recording
5. **Monitor Regularly**: Check dashboards daily
6. **Review Alerts**: Tune thresholds based on actual behavior
7. **Document**: Update metrics reference when adding new metrics

---

### Getting Help

- **Setup Guide**: See `docs/MONITORING_SETUP.md`
- **Metrics Reference**: See `docs/METRICS_REFERENCE.md`
- **Full Report**: See `MONITORING_IMPLEMENTATION_REPORT.md`
- **Prometheus Docs**: https://prometheus.io/docs/
- **Micrometer Docs**: https://micrometer.io/docs

---

**Questions?** Check the comprehensive documentation in the `docs/` folder!
