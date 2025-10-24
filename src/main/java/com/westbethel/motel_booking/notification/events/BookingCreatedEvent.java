package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Event published when a new booking is created.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookingCreatedEvent extends NotificationEvent {

    private String firstName;
    private String confirmationNumber;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfGuests;
    private String specialRequests;
    private BigDecimal totalAmount;

    @Builder
    public BookingCreatedEvent(String userId, String email, String firstName,
                              String confirmationNumber, String roomType,
                              LocalDate checkInDate, LocalDate checkOutDate,
                              Integer numberOfNights, Integer numberOfGuests,
                              String specialRequests, BigDecimal totalAmount) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.confirmationNumber = confirmationNumber;
        this.roomType = roomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfNights = numberOfNights;
        this.numberOfGuests = numberOfGuests;
        this.specialRequests = specialRequests;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getTemplateName() {
        return "booking-confirmation";
    }
}
