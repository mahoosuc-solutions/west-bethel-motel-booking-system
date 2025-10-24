package com.westbethel.motel_booking.monitoring.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard Controller
 *
 * Provides real-time metrics and system health endpoints for admin dashboard.
 * All endpoints require ADMIN role.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Real-time monitoring and metrics dashboard")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get current dashboard metrics
     *
     * @return Dashboard metrics including business and infrastructure data
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get dashboard metrics",
        description = "Returns real-time metrics for the admin dashboard including business metrics, infrastructure status, and recent activity"
    )
    public ResponseEntity<DashboardMetrics> getMetrics() {
        log.debug("Admin dashboard metrics requested");

        DashboardMetrics metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get system health status
     *
     * @return Detailed system health information
     */
    @GetMapping("/system-health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get system health",
        description = "Returns detailed health status of all system components including database, Redis, email service, and memory"
    )
    public ResponseEntity<SystemHealth> getSystemHealth() {
        log.debug("System health status requested");

        SystemHealth health = dashboardService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * Get recent activity summary
     *
     * @return Recent bookings and errors
     */
    @GetMapping("/recent-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get recent activity",
        description = "Returns recent bookings and error events for quick monitoring"
    )
    public ResponseEntity<RecentActivityResponse> getRecentActivity() {
        log.debug("Recent activity requested");

        DashboardMetrics metrics = dashboardService.getDashboardMetrics();

        RecentActivityResponse response = RecentActivityResponse.builder()
            .recentBookings(metrics.getRecentBookings())
            .recentErrors(metrics.getRecentErrors())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Recent Activity Response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecentActivityResponse {
        private java.util.List<DashboardMetrics.RecentBooking> recentBookings;
        private java.util.List<DashboardMetrics.RecentError> recentErrors;
    }
}
