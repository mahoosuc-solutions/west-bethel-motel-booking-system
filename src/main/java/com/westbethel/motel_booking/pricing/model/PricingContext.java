package com.westbethel.motel_booking.pricing.model;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PricingContext {

    private final UUID propertyId;
    private final UUID ratePlanId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final Integer adults;
    private final Integer children;
    private final UUID guestId;
    private final Set<UUID> roomTypeIds;
}
