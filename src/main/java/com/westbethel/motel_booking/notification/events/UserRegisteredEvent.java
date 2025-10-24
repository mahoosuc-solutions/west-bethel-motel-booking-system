package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when a new user registers.
 * Triggers welcome email with verification link.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends NotificationEvent {

    private String firstName;
    private String lastName;
    private String verificationLink;

    @Builder
    public UserRegisteredEvent(String userId, String email, String firstName, String lastName, String verificationLink) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.verificationLink = verificationLink;
    }

    @Override
    public String getTemplateName() {
        return "welcome-email";
    }
}
