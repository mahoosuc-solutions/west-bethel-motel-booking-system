package com.westbethel.motel_booking.loyalty.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoyaltyRedemptionRequest {

    private final UUID guestId;
    private final UUID bookingId;
    private final Long points;
    private final String reason;
}
