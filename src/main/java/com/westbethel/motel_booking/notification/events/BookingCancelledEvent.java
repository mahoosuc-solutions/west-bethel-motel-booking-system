package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Event published when a booking is cancelled.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookingCancelledEvent extends NotificationEvent {

    private String firstName;
    private String confirmationNumber;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private OffsetDateTime cancelledAt;
    private BigDecimal refundAmount;
    private String refundReference;
    private BigDecimal cancellationFee;

    @Builder
    public BookingCancelledEvent(String userId, String email, String firstName,
                                String confirmationNumber, String roomType,
                                LocalDate checkInDate, LocalDate checkOutDate,
                                BigDecimal refundAmount, String refundReference,
                                BigDecimal cancellationFee) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.confirmationNumber = confirmationNumber;
        this.roomType = roomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.cancelledAt = OffsetDateTime.now();
        this.refundAmount = refundAmount;
        this.refundReference = refundReference;
        this.cancellationFee = cancellationFee;
    }

    @Override
    public String getTemplateName() {
        return "booking-cancelled";
    }
}
