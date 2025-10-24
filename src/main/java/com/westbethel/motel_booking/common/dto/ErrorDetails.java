package com.westbethel.motel_booking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional context for error responses.
 * Used to provide detailed information about the error without exposing sensitive data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {

    /**
     * Additional contextual information about the error
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * Suggested actions to resolve the error
     */
    private String suggestion;

    /**
     * Documentation URL for more information
     */
    private String documentationUrl;

    /**
     * Add a context entry
     */
    public ErrorDetails addContext(String key, Object value) {
        if (this.context == null) {
            this.context = new HashMap<>();
        }
        this.context.put(key, value);
        return this;
    }

    /**
     * Create error details with a suggestion
     */
    public static ErrorDetails withSuggestion(String suggestion) {
        return ErrorDetails.builder()
                .suggestion(suggestion)
                .build();
    }
}
