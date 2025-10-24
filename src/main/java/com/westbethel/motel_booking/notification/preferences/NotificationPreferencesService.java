package com.westbethel.motel_booking.notification.preferences;

import com.westbethel.motel_booking.exception.ResourceNotFoundException;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user notification preferences.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    /**
     * Gets notification preferences for a user.
     * Creates default preferences if they don't exist.
     */
    @Transactional(readOnly = true)
    public NotificationPreferences getPreferences(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Updates notification preferences for a user.
     */
    @Transactional
    public NotificationPreferences updatePreferences(UUID userId, NotificationPreferencesUpdateRequest request) {
        NotificationPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        // Update preferences
        if (request.getEmailEnabled() != null) {
            preferences.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getBookingConfirmations() != null) {
            preferences.setBookingConfirmations(request.getBookingConfirmations());
        }
        if (request.getPaymentReceipts() != null) {
            preferences.setPaymentReceipts(request.getPaymentReceipts());
        }
        if (request.getLoyaltyUpdates() != null) {
            preferences.setLoyaltyUpdates(request.getLoyaltyUpdates());
        }
        if (request.getPromotionalEmails() != null) {
            preferences.setPromotionalEmails(request.getPromotionalEmails());
        }
        if (request.getSmsEnabled() != null) {
            preferences.setSmsEnabled(request.getSmsEnabled());
        }

        // Security alerts cannot be disabled
        preferences.setSecurityAlerts(true);

        NotificationPreferences saved = preferencesRepository.save(preferences);
        log.info("Updated notification preferences for user: {}", userId);
        return saved;
    }

    /**
     * Creates default notification preferences for a user.
     */
    @Transactional
    public NotificationPreferences createDefaultPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (preferencesRepository.existsByUserId(userId)) {
            return preferencesRepository.findByUserId(userId).get();
        }

        NotificationPreferences preferences = NotificationPreferences.createDefault(user);
        NotificationPreferences saved = preferencesRepository.save(preferences);

        log.info("Created default notification preferences for user: {}", userId);
        return saved;
    }

    /**
     * Checks if a specific notification type should be sent based on user preferences.
     *
     * @param userId the user ID
     * @param notificationType the type of notification
     * @return true if the notification should be sent, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean checkPreference(UUID userId, NotificationType notificationType) {
        NotificationPreferences preferences = getPreferences(userId);

        // Security alerts are always sent
        if (notificationType == NotificationType.SECURITY_ALERT) {
            return true;
        }

        // If email is globally disabled, don't send (except security alerts)
        if (!preferences.isEmailEnabled()) {
            return false;
        }

        // Check specific preference
        return switch (notificationType) {
            case BOOKING_CONFIRMATION, BOOKING_CANCELLED -> preferences.isBookingConfirmations();
            case PAYMENT_RECEIPT, PAYMENT_FAILED -> preferences.isPaymentReceipts();
            case LOYALTY_POINTS -> preferences.isLoyaltyUpdates();
            case PROMOTIONAL -> preferences.isPromotionalEmails();
            case SECURITY_ALERT -> true; // Already handled above, but for completeness
            default -> true; // Send by default for other types
        };
    }

    /**
     * Deletes notification preferences for a user.
     */
    @Transactional
    public void deletePreferences(UUID userId) {
        preferencesRepository.deleteByUserId(userId);
        log.info("Deleted notification preferences for user: {}", userId);
    }

    /**
     * Notification type enum for preference checking.
     */
    public enum NotificationType {
        BOOKING_CONFIRMATION,
        BOOKING_CANCELLED,
        PAYMENT_RECEIPT,
        PAYMENT_FAILED,
        LOYALTY_POINTS,
        PROMOTIONAL,
        SECURITY_ALERT,
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        PASSWORD_CHANGED
    }
}
