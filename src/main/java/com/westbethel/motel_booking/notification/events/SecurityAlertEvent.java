package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Event published for security-related alerts.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SecurityAlertEvent extends NotificationEvent {

    private String firstName;
    private String alertType;
    private String alertMessage;
    private boolean isCritical;
    private OffsetDateTime occurredAt;
    private String ipAddress;
    private String location;
    private String device;
    private String browser;
    private boolean wasYou;
    private String secureAccountLink;

    @Builder
    public SecurityAlertEvent(String userId, String email, String firstName,
                             String alertType, String alertMessage, boolean isCritical,
                             String ipAddress, String location, String device,
                             String browser, boolean wasYou, String secureAccountLink) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.alertType = alertType;
        this.alertMessage = alertMessage;
        this.isCritical = isCritical;
        this.occurredAt = OffsetDateTime.now();
        this.ipAddress = ipAddress;
        this.location = location;
        this.device = device;
        this.browser = browser;
        this.wasYou = wasYou;
        this.secureAccountLink = secureAccountLink;
    }

    @Override
    public String getTemplateName() {
        return "security-alert";
    }
}
