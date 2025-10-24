package com.westbethel.motel_booking.availability.model;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailabilityResult {

    private final List<RoomTypeAvailability> roomTypes;

    @Getter
    @Builder
    public static class RoomTypeAvailability {
        private final String roomTypeCode;
        private final Integer availableRooms;
        private final List<NightlyRate> nightlyRates;
    }

    @Getter
    @Builder
    public static class NightlyRate {
        private final LocalDate stayDate;
        private final String currency;
        private final String amount;
    }
}
