package com.westbethel.motel_booking.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Metrics Service Tests")
class MetricsServiceTest {

    private BusinessMetrics businessMetrics;
    private MetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(meterRegistry);
        metricsService = new MetricsService(businessMetrics);
    }

    @Test
    @DisplayName("Should record booking created with amount")
    void shouldRecordBookingCreatedWithAmount() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        metricsService.recordBookingCreated(amount);

        // Then
        var bookingCounter = meterRegistry.find("bookings.created").counter();
        var revenueGauge = meterRegistry.find("revenue.total").gauge();

        assertThat(bookingCounter).isNotNull();
        assertThat(bookingCounter.count()).isEqualTo(1.0);
        assertThat(revenueGauge).isNotNull();
        assertThat(revenueGauge.value()).isEqualTo(10050.0); // $100.50 in cents
    }

    @Test
    @DisplayName("Should record booking cancelled")
    void shouldRecordBookingCancelled() {
        // When
        metricsService.recordBookingCancelled();

        // Then
        var counter = meterRegistry.find("bookings.cancelled").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record payment success")
    void shouldRecordPaymentSuccess() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");

        // When
        metricsService.recordPaymentSuccess(amount);

        // Then
        var counter = meterRegistry.find("payments.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record payment failure")
    void shouldRecordPaymentFailure() {
        // When
        metricsService.recordPaymentFailure("Card declined");

        // Then
        var counter = meterRegistry.find("payments.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record user registration")
    void shouldRecordUserRegistration() {
        // When
        metricsService.recordUserRegistration();

        // Then
        var counter = meterRegistry.find("users.registered").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record authentication success")
    void shouldRecordAuthenticationSuccess() {
        // When
        metricsService.recordAuthenticationSuccess("testuser");

        // Then
        var counter = meterRegistry.find("auth.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record authentication failure")
    void shouldRecordAuthenticationFailure() {
        // When
        metricsService.recordAuthenticationFailure("testuser", "Invalid password");

        // Then
        var counter = meterRegistry.find("auth.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record cache hit")
    void shouldRecordCacheHit() {
        // When
        metricsService.recordCacheHit("userCache");

        // Then
        var counter = meterRegistry.find("cache.hits").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record cache miss")
    void shouldRecordCacheMiss() {
        // When
        metricsService.recordCacheMiss("userCache");

        // Then
        var counter = meterRegistry.find("cache.misses").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record email sent")
    void shouldRecordEmailSent() {
        // When
        metricsService.recordEmailSent("test@example.com");

        // Then
        var counter = meterRegistry.find("emails.sent").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record email failed")
    void shouldRecordEmailFailed() {
        // When
        metricsService.recordEmailFailed("test@example.com", "SMTP error");

        // Then
        var counter = meterRegistry.find("emails.failed").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should update active user sessions")
    void shouldUpdateActiveUserSessions() {
        // When
        metricsService.updateActiveUserSessions(25);

        // Then
        var gauge = meterRegistry.find("sessions.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Should increment and decrement active sessions")
    void shouldIncrementAndDecrementActiveSessions() {
        // Given
        metricsService.updateActiveUserSessions(10);

        // When
        metricsService.incrementActiveUserSessions();
        metricsService.incrementActiveUserSessions();

        // Then
        var gauge = meterRegistry.find("sessions.active").gauge();
        assertThat(gauge.value()).isEqualTo(12.0);

        // When
        metricsService.decrementActiveUserSessions();

        // Then
        assertThat(gauge.value()).isEqualTo(11.0);
    }

    @Test
    @DisplayName("Should update email queue size")
    void shouldUpdateEmailQueueSize() {
        // When
        metricsService.updateEmailQueueSize(150);

        // Then
        var gauge = meterRegistry.find("email.queue.size").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Should record JWT validation time")
    void shouldRecordJwtValidationTime() {
        // When
        metricsService.recordJwtValidationTime(50);

        // Then
        var timer = meterRegistry.find("jwt.validation.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record database query time")
    void shouldRecordDatabaseQueryTime() {
        // When
        metricsService.recordDatabaseQueryTime(200);
        metricsService.recordDatabaseQueryTime(300);

        // Then
        var timer = meterRegistry.find("database.query.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should get cache hit ratio")
    void shouldGetCacheHitRatio() {
        // Given
        metricsService.recordCacheHit("test");
        metricsService.recordCacheHit("test");
        metricsService.recordCacheMiss("test");

        // When
        double ratio = metricsService.getCacheHitRatio();

        // Then
        assertThat(ratio).isEqualTo(2.0 / 3.0);
    }

    @Test
    @DisplayName("Should time operation with timer")
    void shouldTimeOperationWithTimer() {
        // Given
        var timer = businessMetrics.getJwtValidationTimer();

        // When
        String result = metricsService.timeOperation(timer, () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "completed";
        });

        // Then
        assertThat(result).isEqualTo("completed");
        assertThat(timer.count()).isEqualTo(1);
    }
}
