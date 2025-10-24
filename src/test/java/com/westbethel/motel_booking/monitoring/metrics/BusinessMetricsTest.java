package com.westbethel.motel_booking.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Business Metrics Tests")
class BusinessMetricsTest {

    private MeterRegistry meterRegistry;
    private BusinessMetrics businessMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should increment bookings created counter")
    void shouldIncrementBookingsCreated() {
        // When
        businessMetrics.incrementBookingsCreated();
        businessMetrics.incrementBookingsCreated();

        // Then
        var counter = meterRegistry.find("bookings.created").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment bookings cancelled counter")
    void shouldIncrementBookingsCancelled() {
        // When
        businessMetrics.incrementBookingsCancelled();

        // Then
        var counter = meterRegistry.find("bookings.cancelled").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment payment success counter")
    void shouldIncrementPaymentSuccess() {
        // When
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsSuccess();

        // Then
        var counter = meterRegistry.find("payments.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment payment failure counter")
    void shouldIncrementPaymentFailure() {
        // When
        businessMetrics.incrementPaymentsFailure();

        // Then
        var counter = meterRegistry.find("payments.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate payment success rate")
    void shouldCalculatePaymentSuccessRate() {
        // Given
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsFailure();

        // When
        double successRate = businessMetrics.getPaymentSuccessRate();

        // Then
        assertThat(successRate).isEqualTo(0.75); // 3/4 = 75%
    }

    @Test
    @DisplayName("Should increment authentication success counter")
    void shouldIncrementAuthSuccess() {
        // When
        businessMetrics.incrementAuthSuccess();

        // Then
        var counter = meterRegistry.find("auth.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment authentication failure counter")
    void shouldIncrementAuthFailure() {
        // When
        businessMetrics.incrementAuthFailure();

        // Then
        var counter = meterRegistry.find("auth.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate authentication success rate")
    void shouldCalculateAuthSuccessRate() {
        // Given
        businessMetrics.incrementAuthSuccess();
        businessMetrics.incrementAuthSuccess();
        businessMetrics.incrementAuthFailure();

        // When
        double successRate = businessMetrics.getAuthSuccessRate();

        // Then
        assertThat(successRate).isEqualTo(2.0 / 3.0);
    }

    @Test
    @DisplayName("Should increment cache hits counter")
    void shouldIncrementCacheHits() {
        // When
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();

        // Then
        var counter = meterRegistry.find("cache.hits").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment cache misses counter")
    void shouldIncrementCacheMisses() {
        // When
        businessMetrics.incrementCacheMisses();

        // Then
        var counter = meterRegistry.find("cache.misses").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should calculate cache hit ratio")
    void shouldCalculateCacheHitRatio() {
        // Given
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheMisses();

        // When
        double hitRatio = businessMetrics.getCacheHitRatio();

        // Then
        assertThat(hitRatio).isEqualTo(0.75); // 3/4 = 75%
    }

    @Test
    @DisplayName("Should set and get active user sessions")
    void shouldManageActiveUserSessions() {
        // When
        businessMetrics.setActiveUserSessions(10);

        // Then
        var gauge = meterRegistry.find("sessions.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(10.0);

        // When
        businessMetrics.incrementActiveUserSessions();
        businessMetrics.incrementActiveUserSessions();

        // Then
        assertThat(gauge.value()).isEqualTo(12.0);

        // When
        businessMetrics.decrementActiveUserSessions();

        // Then
        assertThat(gauge.value()).isEqualTo(11.0);
    }

    @Test
    @DisplayName("Should set and get email queue size")
    void shouldManageEmailQueueSize() {
        // When
        businessMetrics.setEmailQueueSize(100);

        // Then
        var gauge = meterRegistry.find("email.queue.size").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should add and track revenue")
    void shouldTrackRevenue() {
        // When
        businessMetrics.addRevenue(10000); // $100.00
        businessMetrics.addRevenue(5000);  // $50.00

        // Then
        var gauge = meterRegistry.find("revenue.total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(15000.0); // $150.00 in cents
    }

    @Test
    @DisplayName("Should record JWT validation time")
    void shouldRecordJwtValidationTime() {
        // When
        var timer = businessMetrics.getJwtValidationTimer();
        timer.record(100, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Then
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should record database query time")
    void shouldRecordDatabaseQueryTime() {
        // When
        var timer = businessMetrics.getDatabaseQueryTimer();
        timer.record(250, java.util.concurrent.TimeUnit.MILLISECONDS);
        timer.record(150, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Then
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(400.0);
    }

    @Test
    @DisplayName("Should return zero success rate when no data")
    void shouldReturnZeroSuccessRateWhenNoData() {
        // When & Then
        assertThat(businessMetrics.getPaymentSuccessRate()).isEqualTo(0.0);
        assertThat(businessMetrics.getAuthSuccessRate()).isEqualTo(0.0);
        assertThat(businessMetrics.getCacheHitRatio()).isEqualTo(0.0);
    }
}
