package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Event published when a payment fails.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends NotificationEvent {

    private String firstName;
    private String bookingReference;
    private BigDecimal amount;
    private String paymentMethod;
    private OffsetDateTime attemptedAt;
    private String failureReason;
    private String retryPaymentLink;
    private Integer hoursUntilCancellation;

    @Builder
    public PaymentFailedEvent(String userId, String email, String firstName,
                             String bookingReference, BigDecimal amount,
                             String paymentMethod, String failureReason,
                             String retryPaymentLink, Integer hoursUntilCancellation) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.bookingReference = bookingReference;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.attemptedAt = OffsetDateTime.now();
        this.failureReason = failureReason;
        this.retryPaymentLink = retryPaymentLink;
        this.hoursUntilCancellation = hoursUntilCancellation != null ? hoursUntilCancellation : 24;
    }

    @Override
    public String getTemplateName() {
        return "payment-failed";
    }
}
