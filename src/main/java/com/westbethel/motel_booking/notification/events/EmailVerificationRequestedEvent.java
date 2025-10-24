package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event published when email verification is requested.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailVerificationRequestedEvent extends NotificationEvent {

    private String firstName;
    private String verificationLink;
    private String verificationCode;
    private Integer expiryHours;

    @Builder
    public EmailVerificationRequestedEvent(String userId, String email, String firstName,
                                          String verificationLink, String verificationCode, Integer expiryHours) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.verificationLink = verificationLink;
        this.verificationCode = verificationCode;
        this.expiryHours = expiryHours != null ? expiryHours : 24;
    }

    @Override
    public String getTemplateName() {
        return "email-verification";
    }
}
