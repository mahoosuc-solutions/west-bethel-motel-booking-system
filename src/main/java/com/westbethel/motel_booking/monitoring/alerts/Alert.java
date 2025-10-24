package com.westbethel.motel_booking.monitoring.alerts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alert Model
 *
 * Represents a monitoring alert triggered by threshold violations
 * or critical system events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    /**
     * Alert Severity Levels
     */
    public enum Severity {
        INFO,       // Informational alerts
        WARNING,    // Warning conditions
        CRITICAL    // Critical issues requiring immediate attention
    }

    /**
     * Alert Types
     */
    public enum Type {
        HIGH_ERROR_RATE,
        SLOW_RESPONSE_TIME,
        HIGH_MEMORY_USAGE,
        HIGH_DATABASE_CONNECTIONS,
        HIGH_CACHE_MISS_RATE,
        LARGE_EMAIL_QUEUE,
        FAILED_AUTH_SPIKE,
        PAYMENT_FAILURE_SPIKE,
        DATABASE_CONNECTION_FAILURE,
        REDIS_CONNECTION_FAILURE,
        EMAIL_SERVICE_FAILURE
    }

    private Type type;
    private Severity severity;
    private String title;
    private String message;
    private Double currentValue;
    private Double threshold;
    private String unit;
    private LocalDateTime timestamp;
    private boolean resolved;
    private LocalDateTime resolvedAt;

    /**
     * Create a critical alert
     */
    public static Alert critical(Type type, String title, String message, Double currentValue, Double threshold, String unit) {
        return Alert.builder()
            .type(type)
            .severity(Severity.CRITICAL)
            .title(title)
            .message(message)
            .currentValue(currentValue)
            .threshold(threshold)
            .unit(unit)
            .timestamp(LocalDateTime.now())
            .resolved(false)
            .build();
    }

    /**
     * Create a warning alert
     */
    public static Alert warning(Type type, String title, String message, Double currentValue, Double threshold, String unit) {
        return Alert.builder()
            .type(type)
            .severity(Severity.WARNING)
            .title(title)
            .message(message)
            .currentValue(currentValue)
            .threshold(threshold)
            .unit(unit)
            .timestamp(LocalDateTime.now())
            .resolved(false)
            .build();
    }

    /**
     * Create an info alert
     */
    public static Alert info(Type type, String title, String message) {
        return Alert.builder()
            .type(type)
            .severity(Severity.INFO)
            .title(title)
            .message(message)
            .timestamp(LocalDateTime.now())
            .resolved(false)
            .build();
    }

    /**
     * Mark alert as resolved
     */
    public void resolve() {
        this.resolved = true;
        this.resolvedAt = LocalDateTime.now();
    }
}
