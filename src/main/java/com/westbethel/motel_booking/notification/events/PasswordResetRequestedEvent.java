package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when password reset is requested.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordResetRequestedEvent extends NotificationEvent {

    private String firstName;
    private String resetLink;
    private Integer expiryMinutes;

    @Builder
    public PasswordResetRequestedEvent(String userId, String email, String firstName,
                                      String resetLink, Integer expiryMinutes) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.resetLink = resetLink;
        this.expiryMinutes = expiryMinutes != null ? expiryMinutes : 30;
    }

    @Override
    public String getTemplateName() {
        return "password-reset";
    }
}
