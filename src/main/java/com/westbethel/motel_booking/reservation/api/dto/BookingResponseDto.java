package com.westbethel.motel_booking.reservation.api.dto;

import com.westbethel.motel_booking.common.model.BookingStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingResponseDto {

    private final UUID bookingId;
    private final String confirmationNumber;
    private final BookingStatus status;
}
