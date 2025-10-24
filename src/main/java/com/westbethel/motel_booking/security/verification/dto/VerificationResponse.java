package com.westbethel.motel_booking.security.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for email verification operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private boolean success;
    private String message;
}
