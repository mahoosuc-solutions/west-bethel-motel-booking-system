package com.westbethel.motel_booking.monitoring.dashboard;

import com.westbethel.motel_booking.monitoring.metrics.BusinessMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service
 *
 * Aggregates metrics and health data for the admin dashboard.
 * Provides cached real-time data to minimize performance impact.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BusinessMetrics businessMetrics;
    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    /**
     * Get current dashboard metrics
     * Cached for 10 seconds to reduce load
     */
    @Cacheable(value = "dashboardMetrics", unless = "#result == null")
    public DashboardMetrics getDashboardMetrics() {
        log.debug("Collecting dashboard metrics");

        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long memoryMax = runtime.maxMemory() / (1024 * 1024);
        double memoryUsagePercentage = (double) memoryUsed / memoryMax * 100;

        return DashboardMetrics.builder()
            .activeUsers(getActiveUsersCount())
            .requestsPerSecond(getRequestsPerSecond())
            .averageResponseTimeMs(getAverageResponseTime())
            .errorRate(getErrorRate())
            .totalBookingsToday(getTotalBookingsToday())
            .revenueToday(getRevenueToday())
            .bookingCancellationRate(getBookingCancellationRate())
            .paymentSuccessRate(businessMetrics.getPaymentSuccessRate())
            .databaseConnections(getDatabaseConnections())
            .cacheHitRatio(businessMetrics.getCacheHitRatio())
            .emailQueueSize(getEmailQueueSize())
            .memoryUsedMb(memoryUsed)
            .memoryMaxMb(memoryMax)
            .memoryUsagePercentage(memoryUsagePercentage)
            .cpuUsagePercentage(getCpuUsage())
            .authAttemptsToday(getAuthAttemptsToday())
            .authSuccessRate(businessMetrics.getAuthSuccessRate())
            .activeSessionsCount(getActiveSessionsCount())
            .recentBookings(getRecentBookings())
            .recentErrors(getRecentErrors())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get system health status
     */
    public SystemHealth getSystemHealth() {
        log.debug("Collecting system health information");

        HealthComponent healthComponent = healthEndpoint.health();
        String overallStatus = healthComponent.getStatus().getCode();

        return SystemHealth.builder()
            .overallStatus(overallStatus)
            .timestamp(LocalDateTime.now())
            .database(extractComponentHealth(healthComponent, "db"))
            .redis(extractComponentHealth(healthComponent, "redis"))
            .emailService(extractComponentHealth(healthComponent, "mail"))
            .memory(extractComponentHealth(healthComponent, "memory"))
            .disk(extractComponentHealth(healthComponent, "diskSpace"))
            .additionalDetails(extractAdditionalDetails(healthComponent))
            .build();
    }

    private SystemHealth.ComponentHealth extractComponentHealth(HealthComponent healthComponent, String component) {
        try {
            if (healthComponent instanceof org.springframework.boot.actuate.health.CompositeHealth) {
                org.springframework.boot.actuate.health.CompositeHealth compositeHealth =
                    (org.springframework.boot.actuate.health.CompositeHealth) healthComponent;
                Map<String, HealthComponent> components = compositeHealth.getComponents();

                if (components != null && components.containsKey(component)) {
                    HealthComponent compHealth = components.get(component);
                    Map<String, Object> details = new HashMap<>();

                    if (compHealth instanceof org.springframework.boot.actuate.health.Health) {
                        org.springframework.boot.actuate.health.Health health =
                            (org.springframework.boot.actuate.health.Health) compHealth;
                        details = health.getDetails();
                    }

                    return SystemHealth.ComponentHealth.builder()
                        .status(compHealth.getStatus().getCode())
                        .message(compHealth.getStatus().getDescription())
                        .details(details)
                        .build();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract health for component: {}", component, e);
        }

        return SystemHealth.ComponentHealth.builder()
            .status("UNKNOWN")
            .message("Component health not available")
            .build();
    }

    private Map<String, Object> extractAdditionalDetails(HealthComponent healthComponent) {
        Map<String, Object> details = new HashMap<>();
        try {
            if (healthComponent instanceof org.springframework.boot.actuate.health.CompositeHealth) {
                org.springframework.boot.actuate.health.CompositeHealth compositeHealth =
                    (org.springframework.boot.actuate.health.CompositeHealth) healthComponent;
                Map<String, HealthComponent> components = compositeHealth.getComponents();

                if (components != null) {
                    for (Map.Entry<String, HealthComponent> entry : components.entrySet()) {
                        details.put(entry.getKey(), entry.getValue().getStatus().getCode());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract additional health details", e);
        }
        return details;
    }

    private int getActiveUsersCount() {
        // This would be retrieved from session management
        // For now, return a placeholder
        return 0;
    }

    private double getRequestsPerSecond() {
        try {
            var timer = meterRegistry.find("http.server.requests").timer();
            if (timer != null) {
                return timer.count() / timer.max(java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.debug("Could not calculate requests per second", e);
        }
        return 0.0;
    }

    private long getAverageResponseTime() {
        try {
            var timer = meterRegistry.find("http.server.requests").timer();
            if (timer != null) {
                return (long) timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.debug("Could not calculate average response time", e);
        }
        return 0L;
    }

    private double getErrorRate() {
        try {
            var successCounter = meterRegistry.find("http.server.requests")
                .tag("status", "200")
                .counter();
            var errorCounter = meterRegistry.find("http.server.requests")
                .tag("status", "500")
                .counter();

            if (successCounter != null && errorCounter != null) {
                double total = successCounter.count() + errorCounter.count();
                return total > 0 ? errorCounter.count() / total : 0.0;
            }
        } catch (Exception e) {
            log.debug("Could not calculate error rate", e);
        }
        return 0.0;
    }

    private int getTotalBookingsToday() {
        try {
            var counter = meterRegistry.find("bookings.created").counter();
            return counter != null ? (int) counter.count() : 0;
        } catch (Exception e) {
            log.debug("Could not get bookings count", e);
            return 0;
        }
    }

    private BigDecimal getRevenueToday() {
        try {
            var gauge = meterRegistry.find("revenue.total").gauge();
            if (gauge != null) {
                // Convert from cents to dollars
                return BigDecimal.valueOf(gauge.value() / 100);
            }
        } catch (Exception e) {
            log.debug("Could not get revenue", e);
        }
        return BigDecimal.ZERO;
    }

    private double getBookingCancellationRate() {
        try {
            var created = meterRegistry.find("bookings.created").counter();
            var cancelled = meterRegistry.find("bookings.cancelled").counter();

            if (created != null && cancelled != null) {
                double total = created.count();
                return total > 0 ? cancelled.count() / total : 0.0;
            }
        } catch (Exception e) {
            log.debug("Could not calculate cancellation rate", e);
        }
        return 0.0;
    }

    private int getDatabaseConnections() {
        try {
            var gauge = meterRegistry.find("database.connections.active").gauge();
            return gauge != null ? (int) gauge.value() : 0;
        } catch (Exception e) {
            log.debug("Could not get database connections", e);
            return 0;
        }
    }

    private int getEmailQueueSize() {
        try {
            var gauge = meterRegistry.find("email.queue.size").gauge();
            return gauge != null ? (int) gauge.value() : 0;
        } catch (Exception e) {
            log.debug("Could not get email queue size", e);
            return 0;
        }
    }

    private double getCpuUsage() {
        try {
            var gauge = meterRegistry.find("system.cpu.usage").gauge();
            return gauge != null ? gauge.value() * 100 : 0.0;
        } catch (Exception e) {
            log.debug("Could not get CPU usage", e);
            return 0.0;
        }
    }

    private long getAuthAttemptsToday() {
        try {
            var success = meterRegistry.find("auth.success").counter();
            var failure = meterRegistry.find("auth.failure").counter();

            if (success != null && failure != null) {
                return (long) (success.count() + failure.count());
            }
        } catch (Exception e) {
            log.debug("Could not get auth attempts", e);
        }
        return 0L;
    }

    private int getActiveSessionsCount() {
        try {
            var gauge = meterRegistry.find("sessions.active").gauge();
            return gauge != null ? (int) gauge.value() : 0;
        } catch (Exception e) {
            log.debug("Could not get active sessions", e);
            return 0;
        }
    }

    private List<DashboardMetrics.RecentBooking> getRecentBookings() {
        // This would query the database for recent bookings
        // For now, return empty list
        return new ArrayList<>();
    }

    private List<DashboardMetrics.RecentError> getRecentErrors() {
        // This would retrieve recent errors from logs or monitoring
        // For now, return empty list
        return new ArrayList<>();
    }
}
