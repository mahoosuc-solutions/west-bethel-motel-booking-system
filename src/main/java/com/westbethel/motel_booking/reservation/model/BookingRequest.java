package com.westbethel.motel_booking.reservation.model;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingRequest {

    private final UUID propertyId;
    private final UUID guestId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final Integer adults;
    private final Integer children;
    private final UUID ratePlanId;
    private final Set<UUID> roomTypeIds;
    private final Set<UUID> addonIds;
    private final String paymentToken;
    private final String source;
}
