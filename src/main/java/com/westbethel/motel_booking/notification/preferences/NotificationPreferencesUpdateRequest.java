package com.westbethel.motel_booking.notification.preferences;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesUpdateRequest {

    private Boolean emailEnabled;
    private Boolean bookingConfirmations;
    private Boolean paymentReceipts;
    private Boolean loyaltyUpdates;
    private Boolean promotionalEmails;
    private Boolean smsEnabled;

    // Note: securityAlerts is intentionally omitted as it cannot be disabled by users
}
