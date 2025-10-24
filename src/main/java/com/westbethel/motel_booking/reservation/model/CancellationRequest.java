package com.westbethel.motel_booking.reservation.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancellationRequest {

    private final UUID bookingId;
    private final String confirmationNumber;
    private final String reason;
    private final String requestedBy;
}
