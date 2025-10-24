package com.westbethel.motel_booking.security.mfa;

import com.westbethel.motel_booking.security.mfa.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Multi-Factor Authentication operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;

    /**
     * Setup MFA for the authenticated user.
     *
     * @param authentication the authenticated user
     * @return MFA setup response with secret, QR code, and backup codes
     */
    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        try {
            String username = authentication.getName();
            MfaSetupResponse response = mfaService.setupMfa(username);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("MFA setup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error setting up MFA", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Enable MFA after verifying the initial code.
     *
     * @param request the enable request with verification code
     * @param authentication the authenticated user
     * @return success response
     */
    @PostMapping("/enable")
    public ResponseEntity<MfaResponse> enableMfa(
            @Valid @RequestBody MfaEnableRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            mfaService.enableMfa(username, request.getVerificationCode(), request.getBackupCodes());

            return ResponseEntity.ok(
                    MfaResponse.builder()
                            .success(true)
                            .message("MFA enabled successfully")
                            .build()
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(MfaResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error enabling MFA", e);
            return ResponseEntity.internalServerError()
                    .body(MfaResponse.builder()
                            .success(false)
                            .message("An error occurred while enabling MFA")
                            .build());
        }
    }

    /**
     * Verify MFA code.
     * Used during login after password authentication.
     *
     * @param request the verify request with code
     * @param authentication the authenticated user
     * @return verification response
     */
    @PostMapping("/verify")
    public ResponseEntity<MfaResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            boolean verified = mfaService.verifyMfaCode(username, request.getCode());

            return ResponseEntity.ok(
                    MfaResponse.builder()
                            .success(verified)
                            .message(verified ? "MFA verification successful" : "Invalid code")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error verifying MFA code", e);
            return ResponseEntity.internalServerError()
                    .body(MfaResponse.builder()
                            .success(false)
                            .message("An error occurred during verification")
                            .build());
        }
    }

    /**
     * Disable MFA for the authenticated user.
     * Requires password verification.
     *
     * @param request the disable request with password
     * @param authentication the authenticated user
     * @return success response
     */
    @PostMapping("/disable")
    public ResponseEntity<MfaResponse> disableMfa(
            @Valid @RequestBody MfaDisableRequest request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            mfaService.disableMfa(username, request.getPassword());

            return ResponseEntity.ok(
                    MfaResponse.builder()
                            .success(true)
                            .message("MFA disabled successfully")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(MfaResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error disabling MFA", e);
            return ResponseEntity.internalServerError()
                    .body(MfaResponse.builder()
                            .success(false)
                            .message("An error occurred while disabling MFA")
                            .build());
        }
    }

    /**
     * Regenerate backup codes.
     *
     * @param authentication the authenticated user
     * @return new backup codes
     */
    @PostMapping("/backup-codes")
    public ResponseEntity<BackupCodesResponse> regenerateBackupCodes(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<String> backupCodes = mfaService.regenerateBackupCodes(username);

            return ResponseEntity.ok(
                    BackupCodesResponse.builder()
                            .success(true)
                            .backupCodes(backupCodes)
                            .message("Backup codes regenerated successfully")
                            .build()
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(BackupCodesResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error regenerating backup codes", e);
            return ResponseEntity.internalServerError()
                    .body(BackupCodesResponse.builder()
                            .success(false)
                            .message("An error occurred while regenerating backup codes")
                            .build());
        }
    }
}
