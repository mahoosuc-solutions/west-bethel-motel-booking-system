package com.westbethel.motel_booking.availability.model;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailabilityQuery {

    private final UUID propertyId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer adults;
    private final Integer children;
    private final Set<String> roomTypeCodes;
}
