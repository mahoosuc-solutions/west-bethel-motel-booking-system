package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Event published when a user's password is changed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordChangedEvent extends NotificationEvent {

    private String firstName;
    private OffsetDateTime changedAt;
    private String ipAddress;
    private String device;
    private String location;

    @Builder
    public PasswordChangedEvent(String userId, String email, String firstName,
                               String ipAddress, String device, String location) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.changedAt = OffsetDateTime.now();
        this.ipAddress = ipAddress;
        this.device = device;
        this.location = location;
    }

    @Override
    public String getTemplateName() {
        return "password-changed";
    }
}
