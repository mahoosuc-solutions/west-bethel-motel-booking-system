package com.westbethel.motel_booking.security.mfa;

import com.westbethel.motel_booking.common.audit.AuditEntry;
import com.westbethel.motel_booking.common.service.AuditService;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.mfa.dto.MfaSetupResponse;
import com.westbethel.motel_booking.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Multi-Factor Authentication operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final UserRepository userRepository;
    private final MfaBackupCodeRepository backupCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * Setup MFA for a user.
     * Generates secret and QR code but doesn't enable MFA until verified.
     *
     * @param username the username
     * @return MFA setup response with secret, QR code, and backup codes
     * @throws IllegalStateException if MFA already enabled
     * @throws IOException if QR code generation fails
     */
    @Transactional
    public MfaSetupResponse setupMfa(String username) throws IOException {
        log.info("MFA setup requested for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalStateException("MFA is already enabled");
        }

        // Generate secret
        String secret = totpService.generateSecret();

        // Generate QR code URL
        String qrCodeUrl = totpService.generateQrCodeUrl(username, secret);

        // Generate QR code image
        String qrCodeImage = totpService.generateQrCodeImage(qrCodeUrl);

        // Generate backup codes
        List<String> backupCodes = totpService.generateBackupCodes();

        // Store encrypted secret temporarily (will be confirmed when user verifies first code)
        user.setMfaSecret(secret);
        userRepository.save(user);

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(username)
                .action("MFA_SETUP_INITIATED")
                .performedBy(username)
                .details("MFA setup process initiated")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("MFA setup completed for user: {}", username);

        // Format backup codes for display
        List<String> formattedBackupCodes = backupCodes.stream()
                .map(totpService::formatBackupCode)
                .collect(Collectors.toList());

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .qrCodeImage(qrCodeImage)
                .backupCodes(formattedBackupCodes)
                .build();
    }

    /**
     * Enable MFA after verifying the initial TOTP code.
     * Stores backup codes in hashed form.
     *
     * @param username the username
     * @param verificationCode the TOTP code to verify
     * @param backupCodes the backup codes to store (plaintext)
     * @throws IllegalArgumentException if verification fails
     */
    @Transactional
    public void enableMfa(String username, String verificationCode, List<String> backupCodes) {
        log.info("MFA enable requested for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA setup not initiated. Please setup MFA first.");
        }

        // Verify the code
        if (!totpService.verifyCode(user.getMfaSecret(), verificationCode)) {
            log.warn("Invalid MFA verification code for user: {}", username);
            throw new IllegalArgumentException("Invalid verification code");
        }

        // Enable MFA
        user.setMfaEnabled(true);
        user.setMfaEnabledAt(OffsetDateTime.now());
        userRepository.save(user);

        // Store backup codes (hashed)
        deleteExistingBackupCodes(user);
        storeBackupCodes(user, backupCodes);

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(username)
                .action("MFA_ENABLED")
                .performedBy(username)
                .details("MFA successfully enabled")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("MFA enabled for user: {}", username);
    }

    /**
     * Verify MFA code during login.
     * Supports both TOTP codes and backup codes.
     *
     * @param username the username
     * @param code the code to verify (TOTP or backup code)
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyMfaCode(String username, String code) {
        log.debug("MFA verification for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalStateException("MFA is not enabled for this user");
        }

        // Try TOTP code first
        if (totpService.verifyCode(user.getMfaSecret(), code)) {
            auditService.record(AuditEntry.builder()
                    .id(UUID.randomUUID())
                    .entityType("SECURITY_EVENT")
                    .entityId(username)
                    .action("MFA_VERIFICATION_SUCCESS")
                    .performedBy(username)
                    .details("MFA verification successful (TOTP)")
                    .occurredAt(OffsetDateTime.now())
                    .build());
            return true;
        }

        // Try backup code
        if (verifyAndUseBackupCode(user, code)) {
            auditService.record(AuditEntry.builder()
                    .id(UUID.randomUUID())
                    .entityType("SECURITY_EVENT")
                    .entityId(username)
                    .action("MFA_VERIFICATION_SUCCESS")
                    .performedBy(username)
                    .details("MFA verification successful (Backup Code)")
                    .occurredAt(OffsetDateTime.now())
                    .build());
            return true;
        }

        // Verification failed
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(username)
                .action("MFA_VERIFICATION_FAILED")
                .performedBy(username)
                .details("MFA verification failed")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.warn("MFA verification failed for user: {}", username);
        return false;
    }

    /**
     * Disable MFA for a user.
     * Requires password verification for security.
     *
     * @param username the username
     * @param password the user's password
     * @throws IllegalArgumentException if password is incorrect
     */
    @Transactional
    public void disableMfa(String username, String password) {
        log.info("MFA disable requested for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Invalid password for MFA disable: {}", username);
            throw new IllegalArgumentException("Invalid password");
        }

        // Disable MFA
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaEnabledAt(null);
        userRepository.save(user);

        // Delete backup codes
        deleteExistingBackupCodes(user);

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(username)
                .action("MFA_DISABLED")
                .performedBy(username)
                .details("MFA disabled")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("MFA disabled for user: {}", username);
    }

    /**
     * Regenerate backup codes.
     *
     * @param username the username
     * @return new backup codes
     */
    @Transactional
    public List<String> regenerateBackupCodes(String username) {
        log.info("Backup code regeneration requested for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalStateException("MFA is not enabled");
        }

        // Generate new backup codes
        List<String> backupCodes = totpService.generateBackupCodes();

        // Delete old codes
        deleteExistingBackupCodes(user);

        // Store new codes
        storeBackupCodes(user, backupCodes);

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(username)
                .action("MFA_BACKUP_CODES_REGENERATED")
                .performedBy(username)
                .details("MFA backup codes regenerated")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("Backup codes regenerated for user: {}", username);

        return backupCodes.stream()
                .map(totpService::formatBackupCode)
                .collect(Collectors.toList());
    }

    /**
     * Store backup codes in hashed form.
     *
     * @param user the user
     * @param backupCodes the plaintext backup codes
     */
    private void storeBackupCodes(User user, List<String> backupCodes) {
        for (String code : backupCodes) {
            MfaBackupCode backupCode = MfaBackupCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code.replaceAll("-", "")))
                    .used(false)
                    .build();
            backupCodeRepository.save(backupCode);
        }
        log.debug("Stored {} backup codes for user: {}", backupCodes.size(), user.getUsername());
    }

    /**
     * Delete existing backup codes for a user.
     *
     * @param user the user
     */
    private void deleteExistingBackupCodes(User user) {
        int deleted = backupCodeRepository.deleteByUser(user);
        if (deleted > 0) {
            log.debug("Deleted {} backup codes for user: {}", deleted, user.getUsername());
        }
    }

    /**
     * Verify and use a backup code.
     * Backup codes are single-use.
     *
     * @param user the user
     * @param code the backup code
     * @return true if code is valid and unused
     */
    private boolean verifyAndUseBackupCode(User user, String code) {
        String cleanCode = code.replaceAll("-", "").toUpperCase();

        List<MfaBackupCode> unusedCodes = backupCodeRepository.findUnusedByUser(user);

        for (MfaBackupCode backupCode : unusedCodes) {
            if (passwordEncoder.matches(cleanCode, backupCode.getCodeHash())) {
                backupCode.markAsUsed();
                backupCodeRepository.save(backupCode);
                log.info("Backup code used for user: {}", user.getUsername());
                return true;
            }
        }

        return false;
    }
}
