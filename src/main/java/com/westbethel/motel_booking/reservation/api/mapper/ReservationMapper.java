package com.westbethel.motel_booking.reservation.api.mapper;

import com.westbethel.motel_booking.reservation.api.dto.BookingCreateRequest;
import com.westbethel.motel_booking.reservation.api.dto.BookingResponseDto;
import com.westbethel.motel_booking.reservation.model.BookingRequest;
import com.westbethel.motel_booking.reservation.model.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public BookingRequest toBookingRequest(BookingCreateRequest request) {
        return BookingRequest.builder()
                .propertyId(request.getPropertyId())
                .guestId(request.getGuestId())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .adults(request.getAdults())
                .children(request.getChildren())
                .ratePlanId(request.getRatePlanId())
                .roomTypeIds(request.getRoomTypeIds())
                .addonIds(request.getAddonIds())
                .paymentToken(request.getPaymentToken())
                .source(request.getSource())
                .build();
    }

    public BookingResponseDto toDto(BookingResponse response) {
        return BookingResponseDto.builder()
                .bookingId(response.getBookingId())
                .confirmationNumber(response.getConfirmationNumber())
                .status(response.getStatus())
                .build();
    }
}
