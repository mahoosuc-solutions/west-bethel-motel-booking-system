package com.westbethel.motel_booking.monitoring.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Memory Health Indicator
 *
 * Monitors JVM memory usage and alerts when thresholds are exceeded.
 */
@Slf4j
@Component
public class MemoryHealthIndicator implements HealthIndicator {

    private static final double WARNING_THRESHOLD = 0.75; // 75%
    private static final double CRITICAL_THRESHOLD = 0.90; // 90%

    @Override
    public Health health() {
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double usagePercentage = (double) usedMemory / maxMemory;

        Health.Builder builder;
        String status;

        if (usagePercentage >= CRITICAL_THRESHOLD) {
            builder = Health.down();
            status = "CRITICAL - Memory usage above 90%";
            log.error("Critical memory usage: {}%", String.format("%.2f", usagePercentage * 100));
        } else if (usagePercentage >= WARNING_THRESHOLD) {
            builder = Health.up();
            status = "WARNING - Memory usage above 75%";
            log.warn("High memory usage: {}%", String.format("%.2f", usagePercentage * 100));
        } else {
            builder = Health.up();
            status = "OK";
        }

        return builder
            .withDetail("status", status)
            .withDetail("usedMemory", formatBytes(usedMemory))
            .withDetail("freeMemory", formatBytes(freeMemory))
            .withDetail("totalMemory", formatBytes(totalMemory))
            .withDetail("maxMemory", formatBytes(maxMemory))
            .withDetail("usagePercentage", String.format("%.2f%%", usagePercentage * 100))
            .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}
