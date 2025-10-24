package com.westbethel.motel_booking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error response structure for all API errors.
 * Provides consistent error information to clients while avoiding sensitive data exposure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status reason phrase (e.g., "Bad Request", "Internal Server Error")
     */
    private String error;

    /**
     * Error code for client-side error handling
     */
    private String code;

    /**
     * User-friendly error message
     */
    private String message;

    /**
     * Request path where the error occurred
     */
    private String path;

    /**
     * Correlation ID for tracking requests across services
     */
    private String correlationId;

    /**
     * Validation errors (if applicable)
     */
    private List<ValidationError> validationErrors;

    /**
     * Additional error details
     */
    private Map<String, Object> details;

    /**
     * Create a simple error response
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create an error response with a correlation ID
     */
    public static ErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .build();
    }
}
