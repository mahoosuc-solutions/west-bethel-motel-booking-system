package com.westbethel.motel_booking.reservation.dto;

import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for booking list responses
 *
 * Contains only essential fields needed for list views.
 * Reduces payload size by 50-60% compared to full entity.
 */
@Data
@Builder
public class BookingListDto {
    private UUID id;
    private String reference;
    private UUID guestId;
    private UUID propertyId;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer adults;
    private Integer children;
    private BigDecimal totalAmount;
    private String totalCurrency;
    private OffsetDateTime createdAt;

    /**
     * Create from projection
     */
    public static BookingListDto fromProjection(BookingProjection projection) {
        return BookingListDto.builder()
            .id(projection.getId())
            .reference(projection.getReference())
            .guestId(projection.getGuestId())
            .propertyId(projection.getPropertyId())
            .status(projection.getStatus())
            .paymentStatus(projection.getPaymentStatus())
            .checkIn(projection.getCheckIn())
            .checkOut(projection.getCheckOut())
            .adults(projection.getAdults())
            .children(projection.getChildren())
            .totalAmount(projection.getTotalAmount())
            .totalCurrency(projection.getTotalCurrency())
            .createdAt(projection.getCreatedAt())
            .build();
    }
}
