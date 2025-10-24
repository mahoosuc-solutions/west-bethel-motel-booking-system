package com.westbethel.motel_booking.security.mfa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for backup codes.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupCodesResponse {

    private boolean success;
    private String message;
    private List<String> backupCodes;
}
