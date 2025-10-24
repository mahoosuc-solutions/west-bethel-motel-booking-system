package com.westbethel.motel_booking.monitoring.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Metrics DTO
 *
 * Contains real-time metrics and system health information for the admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

    // Business Metrics
    private int activeUsers;
    private double requestsPerSecond;
    private long averageResponseTimeMs;
    private double errorRate;
    private int totalBookingsToday;
    private BigDecimal revenueToday;
    private double bookingCancellationRate;
    private double paymentSuccessRate;

    // Infrastructure Metrics
    private int databaseConnections;
    private double cacheHitRatio;
    private int emailQueueSize;
    private long memoryUsedMb;
    private long memoryMaxMb;
    private double memoryUsagePercentage;
    private double cpuUsagePercentage;

    // Authentication Metrics
    private long authAttemptsToday;
    private double authSuccessRate;
    private int activeSessionsCount;

    // Recent Activity
    private List<RecentBooking> recentBookings;
    private List<RecentError> recentErrors;

    // Timestamp
    private LocalDateTime timestamp;

    /**
     * Recent Booking DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private Long id;
        private String guestName;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createdAt;
    }

    /**
     * Recent Error DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        private String endpoint;
        private String errorType;
        private String message;
        private LocalDateTime timestamp;
    }
}
