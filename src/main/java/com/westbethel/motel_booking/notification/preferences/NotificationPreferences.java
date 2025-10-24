package com.westbethel.motel_booking.notification.preferences;

import com.westbethel.motel_booking.security.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing user notification preferences.
 * Allows users to control which types of notifications they receive.
 */
@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the user who owns these preferences.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /**
     * Master switch for email notifications.
     * If false, no emails will be sent (except critical security alerts).
     */
    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    /**
     * Receive booking confirmation emails.
     */
    @Column(name = "booking_confirmations", nullable = false)
    @Builder.Default
    private boolean bookingConfirmations = true;

    /**
     * Receive payment receipt emails.
     */
    @Column(name = "payment_receipts", nullable = false)
    @Builder.Default
    private boolean paymentReceipts = true;

    /**
     * Receive loyalty points update emails.
     */
    @Column(name = "loyalty_updates", nullable = false)
    @Builder.Default
    private boolean loyaltyUpdates = true;

    /**
     * Receive promotional and marketing emails.
     */
    @Column(name = "promotional_emails", nullable = false)
    @Builder.Default
    private boolean promotionalEmails = false;

    /**
     * Receive security alert emails.
     * This should always be true and cannot be disabled by users for security reasons.
     */
    @Column(name = "security_alerts", nullable = false)
    @Builder.Default
    private boolean securityAlerts = true;

    /**
     * Master switch for SMS notifications (future feature).
     */
    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private boolean smsEnabled = false;

    /**
     * Timestamp of last update to preferences.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Called before persisting or updating the entity.
     */
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        // Ensure security alerts cannot be disabled
        if (!this.securityAlerts) {
            this.securityAlerts = true;
        }
    }

    /**
     * Creates default notification preferences for a new user.
     */
    public static NotificationPreferences createDefault(User user) {
        return NotificationPreferences.builder()
                .user(user)
                .emailEnabled(true)
                .bookingConfirmations(true)
                .paymentReceipts(true)
                .loyaltyUpdates(true)
                .promotionalEmails(false)
                .securityAlerts(true)
                .smsEnabled(false)
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
