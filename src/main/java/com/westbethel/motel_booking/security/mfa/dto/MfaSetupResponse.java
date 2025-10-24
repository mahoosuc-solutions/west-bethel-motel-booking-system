package com.westbethel.motel_booking.security.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for MFA setup.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {

    private String secret;
    private String qrCodeUrl;
    private String qrCodeImage; // Base64 encoded PNG
    private List<String> backupCodes;
}
