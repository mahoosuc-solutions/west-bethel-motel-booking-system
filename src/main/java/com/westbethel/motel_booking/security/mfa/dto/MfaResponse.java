package com.westbethel.motel_booking.security.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response DTO for MFA operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaResponse {

    private boolean success;
    private String message;
}
