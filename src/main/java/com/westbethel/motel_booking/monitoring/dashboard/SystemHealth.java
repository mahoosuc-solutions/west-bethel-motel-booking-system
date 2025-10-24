package com.westbethel.motel_booking.monitoring.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * System Health DTO
 *
 * Contains detailed system health status for monitoring dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealth {

    private String overallStatus;
    private LocalDateTime timestamp;

    // Component Health
    private ComponentHealth database;
    private ComponentHealth redis;
    private ComponentHealth emailService;
    private ComponentHealth memory;
    private ComponentHealth disk;

    // Additional Details
    private Map<String, Object> additionalDetails;

    /**
     * Component Health Status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String status; // UP, DOWN, WARNING
        private String message;
        private Map<String, Object> details;
        private Long responseTimeMs;
    }
}
