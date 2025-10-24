package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Event published when a payment is received successfully.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentReceivedEvent extends NotificationEvent {

    private String firstName;
    private String receiptNumber;
    private OffsetDateTime transactionDate;
    private String paymentMethod;
    private String bookingReference;
    private String description;
    private BigDecimal amount;
    private String transactionId;
    private Integer loyaltyPointsEarned;

    @Builder
    public PaymentReceivedEvent(String userId, String email, String firstName,
                               String receiptNumber, String paymentMethod,
                               String bookingReference, String description,
                               BigDecimal amount, String transactionId,
                               Integer loyaltyPointsEarned) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.receiptNumber = receiptNumber;
        this.transactionDate = OffsetDateTime.now();
        this.paymentMethod = paymentMethod;
        this.bookingReference = bookingReference;
        this.description = description;
        this.amount = amount;
        this.transactionId = transactionId;
        this.loyaltyPointsEarned = loyaltyPointsEarned;
    }

    @Override
    public String getTemplateName() {
        return "payment-receipt";
    }
}
