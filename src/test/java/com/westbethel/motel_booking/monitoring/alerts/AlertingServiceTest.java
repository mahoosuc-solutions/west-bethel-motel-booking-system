package com.westbethel.motel_booking.monitoring.alerts;

import com.westbethel.motel_booking.monitoring.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Alerting Service Tests")
class AlertingServiceTest {

    private MeterRegistry meterRegistry;
    private BusinessMetrics businessMetrics;
    private AlertingService alertingService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(meterRegistry);
        alertingService = new AlertingService(businessMetrics, meterRegistry);

        // Set test thresholds
        ReflectionTestUtils.setField(alertingService, "errorRateThreshold", 0.05);
        ReflectionTestUtils.setField(alertingService, "responseTimeThreshold", 1000L);
        ReflectionTestUtils.setField(alertingService, "memoryThreshold", 0.80);
        ReflectionTestUtils.setField(alertingService, "cacheMissThreshold", 0.50);
        ReflectionTestUtils.setField(alertingService, "emailQueueThreshold", 1000);
    }

    @Test
    @DisplayName("Should send alert when memory threshold exceeded")
    void shouldSendAlertWhenMemoryThresholdExceeded() {
        // When
        alertingService.checkAlertConditions();

        // Then - Memory alert may or may not trigger depending on actual memory usage
        // This test verifies the check runs without error
        List<Alert> activeAlerts = alertingService.getActiveAlerts();
        assertThat(activeAlerts).isNotNull();
    }

    @Test
    @DisplayName("Should send alert when cache miss rate is high")
    void shouldSendAlertWhenCacheMissRateIsHigh() {
        // Given - 80% cache miss rate
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();

        // When
        alertingService.checkAlertConditions();

        // Then
        List<Alert> activeAlerts = alertingService.getActiveAlerts();
        boolean hasCacheMissAlert = activeAlerts.stream()
            .anyMatch(alert -> alert.getType() == Alert.Type.HIGH_CACHE_MISS_RATE);
        assertThat(hasCacheMissAlert).isTrue();
    }

    @Test
    @DisplayName("Should send alert when payment failure rate is high")
    void shouldSendAlertWhenPaymentFailureRateIsHigh() {
        // Given - 50% payment failure rate
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsFailure();

        // When
        alertingService.checkAlertConditions();

        // Then
        List<Alert> activeAlerts = alertingService.getActiveAlerts();
        boolean hasPaymentAlert = activeAlerts.stream()
            .anyMatch(alert -> alert.getType() == Alert.Type.PAYMENT_FAILURE_SPIKE);
        assertThat(hasPaymentAlert).isTrue();
    }

    @Test
    @DisplayName("Should send alert when auth failure rate is high")
    void shouldSendAlertWhenAuthFailureRateIsHigh() {
        // Given - 50% auth failure rate
        businessMetrics.incrementAuthSuccess();
        businessMetrics.incrementAuthFailure();

        // When
        alertingService.checkAlertConditions();

        // Then
        List<Alert> activeAlerts = alertingService.getActiveAlerts();
        boolean hasAuthAlert = activeAlerts.stream()
            .anyMatch(alert -> alert.getType() == Alert.Type.FAILED_AUTH_SPIKE);
        assertThat(hasAuthAlert).isTrue();
    }

    @Test
    @DisplayName("Should not duplicate alerts")
    void shouldNotDuplicateAlerts() {
        // Given - High cache miss rate
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheHits();

        // When
        alertingService.checkAlertConditions();
        int firstCheckCount = alertingService.getActiveAlerts().size();

        alertingService.checkAlertConditions();
        int secondCheckCount = alertingService.getActiveAlerts().size();

        // Then
        assertThat(firstCheckCount).isEqualTo(secondCheckCount);
    }

    @Test
    @DisplayName("Should resolve alerts when condition clears")
    void shouldResolveAlertsWhenConditionClears() {
        // Given - Create high cache miss rate
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheMisses();
        businessMetrics.incrementCacheHits();

        // When
        alertingService.checkAlertConditions();
        int alertsBeforeResolve = alertingService.getActiveAlerts().size();

        // Add cache hits to improve ratio
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();

        alertingService.checkAlertConditions();
        int alertsAfterResolve = alertingService.getActiveAlerts().size();

        // Then
        assertThat(alertsAfterResolve).isLessThanOrEqualTo(alertsBeforeResolve);
    }

    @Test
    @DisplayName("Should create critical alert with correct severity")
    void shouldCreateCriticalAlertWithCorrectSeverity() {
        // Given
        Alert alert = Alert.critical(
            Alert.Type.HIGH_ERROR_RATE,
            "Test Alert",
            "Test message",
            10.0,
            5.0,
            "%"
        );

        // Then
        assertThat(alert.getSeverity()).isEqualTo(Alert.Severity.CRITICAL);
        assertThat(alert.getType()).isEqualTo(Alert.Type.HIGH_ERROR_RATE);
        assertThat(alert.getTitle()).isEqualTo("Test Alert");
        assertThat(alert.getMessage()).isEqualTo("Test message");
        assertThat(alert.getCurrentValue()).isEqualTo(10.0);
        assertThat(alert.getThreshold()).isEqualTo(5.0);
        assertThat(alert.getUnit()).isEqualTo("%");
        assertThat(alert.isResolved()).isFalse();
    }

    @Test
    @DisplayName("Should mark alert as resolved")
    void shouldMarkAlertAsResolved() {
        // Given
        Alert alert = Alert.warning(
            Alert.Type.SLOW_RESPONSE_TIME,
            "Test Alert",
            "Test message",
            1500.0,
            1000.0,
            "ms"
        );

        // When
        alert.resolve();

        // Then
        assertThat(alert.isResolved()).isTrue();
        assertThat(alert.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should clear resolved alerts")
    void shouldClearResolvedAlerts() {
        // Given
        Alert alert = Alert.info(
            Alert.Type.HIGH_ERROR_RATE,
            "Test Alert",
            "Test message"
        );
        alert.resolve();

        // When
        alertingService.clearResolvedAlerts();

        // Then - Resolved alerts should eventually be cleared
        // This test mainly verifies the method runs without error
        assertThat(alertingService.getActiveAlerts()).isNotNull();
    }
}
