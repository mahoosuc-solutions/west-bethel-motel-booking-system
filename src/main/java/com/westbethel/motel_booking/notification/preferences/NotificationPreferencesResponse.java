package com.westbethel.motel_booking.notification.preferences;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {

    private UUID id;
    private UUID userId;
    private boolean emailEnabled;
    private boolean bookingConfirmations;
    private boolean paymentReceipts;
    private boolean loyaltyUpdates;
    private boolean promotionalEmails;
    private boolean securityAlerts;
    private boolean smsEnabled;
    private OffsetDateTime updatedAt;

    /**
     * Converts entity to response DTO.
     */
    public static NotificationPreferencesResponse fromEntity(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUser().getId())
                .emailEnabled(preferences.isEmailEnabled())
                .bookingConfirmations(preferences.isBookingConfirmations())
                .paymentReceipts(preferences.isPaymentReceipts())
                .loyaltyUpdates(preferences.isLoyaltyUpdates())
                .promotionalEmails(preferences.isPromotionalEmails())
                .securityAlerts(preferences.isSecurityAlerts())
                .smsEnabled(preferences.isSmsEnabled())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
