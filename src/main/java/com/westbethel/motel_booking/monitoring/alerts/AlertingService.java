package com.westbethel.motel_booking.monitoring.alerts;

import com.westbethel.motel_booking.monitoring.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Alerting Service
 *
 * Monitors system metrics and triggers alerts when thresholds are exceeded.
 * Sends notifications to administrators via email and logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertingService {

    private final BusinessMetrics businessMetrics;
    private final MeterRegistry meterRegistry;

    // Alert thresholds (configurable via properties)
    @Value("${monitoring.alerts.error-rate-threshold:0.05}")
    private double errorRateThreshold;

    @Value("${monitoring.alerts.response-time-threshold:1000}")
    private long responseTimeThreshold;

    @Value("${monitoring.alerts.memory-threshold:0.80}")
    private double memoryThreshold;

    @Value("${monitoring.alerts.db-pool-threshold:0.80}")
    private double dbPoolThreshold;

    @Value("${monitoring.alerts.cache-miss-threshold:0.50}")
    private double cacheMissThreshold;

    @Value("${monitoring.alerts.email-queue-threshold:1000}")
    private int emailQueueThreshold;

    // Track active alerts to avoid spam
    private final ConcurrentHashMap<Alert.Type, Alert> activeAlerts = new ConcurrentHashMap<>();

    /**
     * Check alert conditions every minute
     */
    @Scheduled(fixedDelay = 60000) // Every 60 seconds
    public void checkAlertConditions() {
        log.trace("Checking alert conditions");

        try {
            checkErrorRate();
            checkResponseTime();
            checkMemoryUsage();
            checkCacheMissRate();
            checkEmailQueueSize();
            checkPaymentFailures();
            checkAuthenticationFailures();
        } catch (Exception e) {
            log.error("Error checking alert conditions", e);
        }
    }

    /**
     * Check error rate
     */
    private void checkErrorRate() {
        try {
            var successCounter = meterRegistry.find("http.server.requests")
                .tag("status", "200")
                .counter();
            var errorCounter = meterRegistry.find("http.server.requests")
                .tag("status", "500")
                .counter();

            if (successCounter != null && errorCounter != null) {
                double total = successCounter.count() + errorCounter.count();
                if (total > 0) {
                    double errorRate = errorCounter.count() / total;

                    if (errorRate > errorRateThreshold) {
                        Alert alert = Alert.critical(
                            Alert.Type.HIGH_ERROR_RATE,
                            "High Error Rate Detected",
                            String.format("Error rate is %.2f%%, exceeding threshold of %.2f%%",
                                errorRate * 100, errorRateThreshold * 100),
                            errorRate * 100,
                            errorRateThreshold * 100,
                            "%"
                        );
                        sendAlert(alert);
                    } else {
                        resolveAlert(Alert.Type.HIGH_ERROR_RATE);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not check error rate", e);
        }
    }

    /**
     * Check response time
     */
    private void checkResponseTime() {
        try {
            var timer = meterRegistry.find("http.server.requests").timer();
            if (timer != null) {
                long avgResponseTime = (long) timer.mean(TimeUnit.MILLISECONDS);

                if (avgResponseTime > responseTimeThreshold) {
                    Alert alert = Alert.warning(
                        Alert.Type.SLOW_RESPONSE_TIME,
                        "Slow Response Time",
                        String.format("Average response time is %dms, exceeding threshold of %dms",
                            avgResponseTime, responseTimeThreshold),
                        (double) avgResponseTime,
                        (double) responseTimeThreshold,
                        "ms"
                    );
                    sendAlert(alert);
                } else {
                    resolveAlert(Alert.Type.SLOW_RESPONSE_TIME);
                }
            }
        } catch (Exception e) {
            log.debug("Could not check response time", e);
        }
    }

    /**
     * Check memory usage
     */
    private void checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double usagePercentage = (double) usedMemory / maxMemory;

        if (usagePercentage > memoryThreshold) {
            Alert alert = Alert.critical(
                Alert.Type.HIGH_MEMORY_USAGE,
                "High Memory Usage",
                String.format("Memory usage is %.2f%%, exceeding threshold of %.2f%%",
                    usagePercentage * 100, memoryThreshold * 100),
                usagePercentage * 100,
                memoryThreshold * 100,
                "%"
            );
            sendAlert(alert);
        } else {
            resolveAlert(Alert.Type.HIGH_MEMORY_USAGE);
        }
    }

    /**
     * Check cache miss rate
     */
    private void checkCacheMissRate() {
        double cacheHitRatio = businessMetrics.getCacheHitRatio();
        double cacheMissRate = 1.0 - cacheHitRatio;

        if (cacheMissRate > cacheMissThreshold && cacheHitRatio > 0) {
            Alert alert = Alert.warning(
                Alert.Type.HIGH_CACHE_MISS_RATE,
                "High Cache Miss Rate",
                String.format("Cache miss rate is %.2f%%, exceeding threshold of %.2f%%",
                    cacheMissRate * 100, cacheMissThreshold * 100),
                cacheMissRate * 100,
                cacheMissThreshold * 100,
                "%"
            );
            sendAlert(alert);
        } else {
            resolveAlert(Alert.Type.HIGH_CACHE_MISS_RATE);
        }
    }

    /**
     * Check email queue size
     */
    private void checkEmailQueueSize() {
        try {
            var gauge = meterRegistry.find("email.queue.size").gauge();
            if (gauge != null) {
                int queueSize = (int) gauge.value();

                if (queueSize > emailQueueThreshold) {
                    Alert alert = Alert.warning(
                        Alert.Type.LARGE_EMAIL_QUEUE,
                        "Large Email Queue",
                        String.format("Email queue size is %d, exceeding threshold of %d",
                            queueSize, emailQueueThreshold),
                        (double) queueSize,
                        (double) emailQueueThreshold,
                        "emails"
                    );
                    sendAlert(alert);
                } else {
                    resolveAlert(Alert.Type.LARGE_EMAIL_QUEUE);
                }
            }
        } catch (Exception e) {
            log.debug("Could not check email queue size", e);
        }
    }

    /**
     * Check payment failure rate
     */
    private void checkPaymentFailures() {
        double paymentSuccessRate = businessMetrics.getPaymentSuccessRate();

        if (paymentSuccessRate < 0.90 && paymentSuccessRate > 0) {
            Alert alert = Alert.critical(
                Alert.Type.PAYMENT_FAILURE_SPIKE,
                "High Payment Failure Rate",
                String.format("Payment success rate is %.2f%%, below threshold of 90%%",
                    paymentSuccessRate * 100),
                paymentSuccessRate * 100,
                90.0,
                "%"
            );
            sendAlert(alert);
        } else {
            resolveAlert(Alert.Type.PAYMENT_FAILURE_SPIKE);
        }
    }

    /**
     * Check authentication failure rate
     */
    private void checkAuthenticationFailures() {
        double authSuccessRate = businessMetrics.getAuthSuccessRate();

        if (authSuccessRate < 0.80 && authSuccessRate > 0) {
            Alert alert = Alert.warning(
                Alert.Type.FAILED_AUTH_SPIKE,
                "High Authentication Failure Rate",
                String.format("Authentication success rate is %.2f%%, below threshold of 80%%",
                    authSuccessRate * 100),
                authSuccessRate * 100,
                80.0,
                "%"
            );
            sendAlert(alert);
        } else {
            resolveAlert(Alert.Type.FAILED_AUTH_SPIKE);
        }
    }

    /**
     * Send alert notification
     */
    public void sendAlert(Alert alert) {
        // Check if alert is already active to avoid spam
        Alert existingAlert = activeAlerts.get(alert.getType());
        if (existingAlert != null && !existingAlert.isResolved()) {
            log.trace("Alert already active: {}", alert.getType());
            return;
        }

        // Log the alert
        switch (alert.getSeverity()) {
            case CRITICAL:
                log.error("CRITICAL ALERT: {} - {}", alert.getTitle(), alert.getMessage());
                break;
            case WARNING:
                log.warn("WARNING ALERT: {} - {}", alert.getTitle(), alert.getMessage());
                break;
            case INFO:
                log.info("INFO ALERT: {} - {}", alert.getTitle(), alert.getMessage());
                break;
        }

        // Store active alert
        activeAlerts.put(alert.getType(), alert);

        // TODO: Send email to administrators
        // TODO: Integration with Slack, PagerDuty, etc.
    }

    /**
     * Resolve an active alert
     */
    private void resolveAlert(Alert.Type type) {
        Alert alert = activeAlerts.get(type);
        if (alert != null && !alert.isResolved()) {
            alert.resolve();
            log.info("Alert resolved: {}", type);
        }
    }

    /**
     * Get all active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values()).stream()
            .filter(alert -> !alert.isResolved())
            .toList();
    }

    /**
     * Clear all resolved alerts
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    public void clearResolvedAlerts() {
        activeAlerts.entrySet().removeIf(entry -> entry.getValue().isResolved());
        log.debug("Cleared resolved alerts");
    }
}
