package com.westbethel.motel_booking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single validation error for a field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationError {

    /**
     * The field that failed validation
     */
    private String field;

    /**
     * The rejected value (sanitized to avoid exposing sensitive data)
     */
    private Object rejectedValue;

    /**
     * User-friendly validation error message
     */
    private String message;

    /**
     * Create a validation error with field and message
     */
    public static ValidationError of(String field, String message) {
        return ValidationError.builder()
                .field(field)
                .message(message)
                .build();
    }

    /**
     * Create a validation error with field, rejected value, and message
     */
    public static ValidationError of(String field, Object rejectedValue, String message) {
        return ValidationError.builder()
                .field(field)
                .rejectedValue(sanitizeValue(rejectedValue))
                .message(message)
                .build();
    }

    /**
     * Sanitize sensitive values from error responses
     */
    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();
        String lowerValue = stringValue.toLowerCase();

        // Don't expose sensitive data
        if (lowerValue.contains("password") ||
            lowerValue.contains("token") ||
            lowerValue.contains("secret") ||
            lowerValue.contains("credit") ||
            lowerValue.contains("card")) {
            return "***";
        }

        return value;
    }
}
