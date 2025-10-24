package com.westbethel.motel_booking.notification.email;

import com.westbethel.motel_booking.notification.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Email service for security-related notifications.
 * This service is designed to be used by the security module (Agent 1).
 * It publishes events that are handled by EmailNotificationListener.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityEmailService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Sends password reset email.
     *
     * @param email user's email address
     * @param resetToken the password reset token
     */
    public void sendPasswordResetEmail(String email, String resetToken) {
        sendPasswordResetEmail(email, resetToken, "User", 30);
    }

    /**
     * Sends password reset email with custom parameters.
     *
     * @param email user's email address
     * @param resetToken the password reset token
     * @param firstName user's first name
     * @param expiryMinutes expiry time in minutes
     */
    public void sendPasswordResetEmail(String email, String resetToken, String firstName, Integer expiryMinutes) {
        String resetLink = generatePasswordResetLink(resetToken);

        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.builder()
                .userId(null) // Can be set if available
                .email(email)
                .firstName(firstName)
                .resetLink(resetLink)
                .expiryMinutes(expiryMinutes)
                .build();

        eventPublisher.publishEvent(event);
        log.info("Password reset email event published for: {}", email);
    }

    /**
     * Sends email verification email.
     *
     * @param email user's email address
     * @param verificationToken the email verification token
     */
    public void sendEmailVerification(String email, String verificationToken) {
        sendEmailVerification(email, verificationToken, "User", 24);
    }

    /**
     * Sends email verification email with custom parameters.
     *
     * @param email user's email address
     * @param verificationToken the email verification token
     * @param firstName user's first name
     * @param expiryHours expiry time in hours
     */
    public void sendEmailVerification(String email, String verificationToken, String firstName, Integer expiryHours) {
        String verificationLink = generateEmailVerificationLink(verificationToken);

        EmailVerificationRequestedEvent event = EmailVerificationRequestedEvent.builder()
                .userId(null) // Can be set if available
                .email(email)
                .firstName(firstName)
                .verificationLink(verificationLink)
                .verificationCode(extractVerificationCode(verificationToken))
                .expiryHours(expiryHours)
                .build();

        eventPublisher.publishEvent(event);
        log.info("Email verification event published for: {}", email);
    }

    /**
     * Sends password changed alert email.
     *
     * @param email user's email address
     */
    public void sendPasswordChangedAlert(String email) {
        sendPasswordChangedAlert(email, "User", null, null, null);
    }

    /**
     * Sends password changed alert email with details.
     *
     * @param email user's email address
     * @param firstName user's first name
     * @param ipAddress IP address where change occurred
     * @param device device information
     * @param location location information
     */
    public void sendPasswordChangedAlert(String email, String firstName, String ipAddress, String device, String location) {
        PasswordChangedEvent event = PasswordChangedEvent.builder()
                .userId(null) // Can be set if available
                .email(email)
                .firstName(firstName)
                .ipAddress(ipAddress)
                .device(device)
                .location(location)
                .build();

        eventPublisher.publishEvent(event);
        log.info("Password changed alert event published for: {}", email);
    }

    /**
     * Sends MFA setup email.
     *
     * @param email user's email address
     * @param secret MFA secret key
     * @param qrCodeUrl URL to QR code image
     */
    public void sendMfaSetupEmail(String email, String secret, String qrCodeUrl) {
        sendMfaSetupEmail(email, secret, qrCodeUrl, "User");
    }

    /**
     * Sends MFA setup email with custom parameters.
     *
     * @param email user's email address
     * @param secret MFA secret key
     * @param qrCodeUrl URL to QR code image
     * @param firstName user's first name
     */
    public void sendMfaSetupEmail(String email, String secret, String qrCodeUrl, String firstName) {
        // Create security alert for MFA setup
        SecurityAlertEvent event = SecurityAlertEvent.builder()
                .userId(null)
                .email(email)
                .firstName(firstName)
                .alertType("MFA Setup")
                .alertMessage("Two-factor authentication has been enabled on your account.")
                .isCritical(false)
                .wasYou(true)
                .build();

        event.addMetadata("secret", secret);
        event.addMetadata("qrCodeUrl", qrCodeUrl);

        eventPublisher.publishEvent(event);
        log.info("MFA setup email event published for: {}", email);
    }

    /**
     * Sends security alert email.
     *
     * @param email user's email address
     * @param alertType type of security alert
     * @param details additional alert details
     */
    public void sendSecurityAlert(String email, String alertType, Map<String, Object> details) {
        String firstName = (String) details.getOrDefault("firstName", "User");
        String message = (String) details.getOrDefault("message", "Suspicious activity detected on your account.");
        Boolean isCritical = (Boolean) details.getOrDefault("isCritical", false);
        String ipAddress = (String) details.get("ipAddress");
        String device = (String) details.get("device");
        String location = (String) details.get("location");
        String browser = (String) details.get("browser");
        Boolean wasYou = (Boolean) details.getOrDefault("wasYou", false);

        SecurityAlertEvent event = SecurityAlertEvent.builder()
                .userId(null)
                .email(email)
                .firstName(firstName)
                .alertType(alertType)
                .alertMessage(message)
                .isCritical(isCritical)
                .ipAddress(ipAddress)
                .device(device)
                .location(location)
                .browser(browser)
                .wasYou(wasYou)
                .secureAccountLink(generateSecureAccountLink())
                .build();

        eventPublisher.publishEvent(event);
        log.info("Security alert event published for: {} (Type: {})", email, alertType);
    }

    /**
     * Sends a welcome email for new user registration.
     *
     * @param email user's email address
     * @param firstName user's first name
     * @param lastName user's last name
     * @param verificationToken email verification token
     */
    public void sendWelcomeEmail(String email, String firstName, String lastName, String verificationToken) {
        String verificationLink = generateEmailVerificationLink(verificationToken);

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(null)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .verificationLink(verificationLink)
                .build();

        eventPublisher.publishEvent(event);
        log.info("Welcome email event published for: {}", email);
    }

    // Helper methods to generate links (these should be configured based on your frontend URL)

    private String generatePasswordResetLink(String token) {
        // In production, this should use a configured frontend URL
        String baseUrl = System.getenv("FRONTEND_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3000"; // Default for development
        }
        return baseUrl + "/reset-password?token=" + token;
    }

    private String generateEmailVerificationLink(String token) {
        String baseUrl = System.getenv("FRONTEND_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3000";
        }
        return baseUrl + "/verify-email?token=" + token;
    }

    private String generateSecureAccountLink() {
        String baseUrl = System.getenv("FRONTEND_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3000";
        }
        return baseUrl + "/account/security";
    }

    private String extractVerificationCode(String token) {
        // Extract a short code from the token for display purposes
        if (token != null && token.length() >= 6) {
            return token.substring(0, 6).toUpperCase();
        }
        return "VERIFY";
    }
}
