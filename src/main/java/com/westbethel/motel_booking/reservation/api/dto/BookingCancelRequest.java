package com.westbethel.motel_booking.reservation.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingCancelRequest {

    @NotBlank
    private String reason;

    @NotBlank
    private String requestedBy;
}
