package com.westbethel.motel_booking.security.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Service for Time-based One-Time Password (TOTP) operations.
 * Implements Google Authenticator compatible 2FA.
 *
 * @author Security Agent 1 - Phase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpService {

    private final GoogleAuthenticator googleAuthenticator;

    @Value("${app.name:West Bethel Motel}")
    private String appName;

    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int BACKUP_CODE_COUNT = 10;
    private static final String BACKUP_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generate a new TOTP secret for a user.
     *
     * @return the secret key
     */
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Generate QR code URL for authenticator app setup.
     * Compatible with Google Authenticator, Authy, etc.
     *
     * @param username the username
     * @param secret the TOTP secret
     * @return QR code URL (otpauth:// format)
     */
    public String generateQrCodeUrl(String username, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                appName,
                username,
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    /**
     * Generate QR code image as Base64 encoded PNG.
     *
     * @param qrCodeUrl the QR code URL
     * @return Base64 encoded PNG image
     * @throws IOException if QR code generation fails
     */
    public String generateQrCodeImage(String qrCodeUrl) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException e) {
            log.error("Failed to generate QR code", e);
            throw new IOException("Failed to generate QR code", e);
        }
    }

    /**
     * Verify a TOTP code against a secret.
     * Accepts codes within a time window to account for clock skew.
     *
     * @param secret the TOTP secret
     * @param code the code to verify (6 digits)
     * @return true if code is valid
     */
    public boolean verifyCode(String secret, String code) {
        try {
            int verificationCode = Integer.parseInt(code);
            return googleAuthenticator.authorize(secret, verificationCode);
        } catch (NumberFormatException e) {
            log.warn("Invalid TOTP code format: {}", code);
            return false;
        }
    }

    /**
     * Generate backup codes for MFA recovery.
     * Generates cryptographically secure random codes.
     *
     * @return list of backup codes (plaintext - must be hashed before storage)
     */
    public List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> backupCodes = new ArrayList<>(BACKUP_CODE_COUNT);

        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                int index = random.nextInt(BACKUP_CODE_CHARS.length());
                code.append(BACKUP_CODE_CHARS.charAt(index));
            }
            backupCodes.add(code.toString());
        }

        log.debug("Generated {} backup codes", BACKUP_CODE_COUNT);
        return backupCodes;
    }

    /**
     * Format backup code for display (adds dash for readability).
     *
     * @param code the backup code
     * @return formatted code (e.g., ABCD-1234)
     */
    public String formatBackupCode(String code) {
        if (code.length() != BACKUP_CODE_LENGTH) {
            return code;
        }
        return code.substring(0, 4) + "-" + code.substring(4);
    }
}
