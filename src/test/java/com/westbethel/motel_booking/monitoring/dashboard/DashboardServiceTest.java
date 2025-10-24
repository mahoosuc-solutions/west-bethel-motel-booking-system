package com.westbethel.motel_booking.monitoring.dashboard;

import com.westbethel.motel_booking.monitoring.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard Service Tests")
class DashboardServiceTest {

    private MeterRegistry meterRegistry;
    private BusinessMetrics businessMetrics;

    @Mock
    private HealthEndpoint healthEndpoint;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(meterRegistry);
        dashboardService = new DashboardService(businessMetrics, meterRegistry, healthEndpoint);
    }

    @Test
    @DisplayName("Should get dashboard metrics")
    void shouldGetDashboardMetrics() {
        // Given
        businessMetrics.incrementBookingsCreated();
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.setActiveUserSessions(10);
        businessMetrics.setEmailQueueSize(50);

        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTimestamp()).isNotNull();
        assertThat(metrics.getMemoryUsedMb()).isGreaterThan(0);
        assertThat(metrics.getMemoryMaxMb()).isGreaterThan(0);
        assertThat(metrics.getMemoryUsagePercentage()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should calculate cache hit ratio correctly")
    void shouldCalculateCacheHitRatioCorrectly() {
        // Given
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheHits();
        businessMetrics.incrementCacheMisses();

        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics.getCacheHitRatio()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("Should get system health")
    void shouldGetSystemHealth() {
        // Given
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("db", Health.up().build());
        healthDetails.put("redis", Health.up().build());

        Health overallHealth = Health.up().withDetails(healthDetails).build();
        when(healthEndpoint.health()).thenReturn(overallHealth);

        // When
        SystemHealth systemHealth = dashboardService.getSystemHealth();

        // Then
        assertThat(systemHealth).isNotNull();
        assertThat(systemHealth.getOverallStatus()).isEqualTo(Status.UP.getCode());
        assertThat(systemHealth.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle missing health components gracefully")
    void shouldHandleMissingHealthComponentsGracefully() {
        // Given
        Health overallHealth = Health.up().build();
        when(healthEndpoint.health()).thenReturn(overallHealth);

        // When
        SystemHealth systemHealth = dashboardService.getSystemHealth();

        // Then
        assertThat(systemHealth).isNotNull();
        assertThat(systemHealth.getDatabase()).isNotNull();
        assertThat(systemHealth.getDatabase().getStatus()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Should include payment success rate in metrics")
    void shouldIncludePaymentSuccessRateInMetrics() {
        // Given
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsSuccess();
        businessMetrics.incrementPaymentsFailure();

        // When
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics.getPaymentSuccessRate()).isEqualTo(0.75);
    }
}
