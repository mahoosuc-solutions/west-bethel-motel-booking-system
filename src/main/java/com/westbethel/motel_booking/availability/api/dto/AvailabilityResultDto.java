package com.westbethel.motel_booking.availability.api.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailabilityResultDto {

    private final List<RoomTypeAvailabilityDto> roomTypes;

    @Getter
    @Builder
    public static class RoomTypeAvailabilityDto {
        private final String roomTypeCode;
        private final String roomTypeName;
        private final Integer availableRooms;
        private final List<NightlyRateDto> nightlyRates;
    }

    @Getter
    @Builder
    public static class NightlyRateDto {
        private final LocalDate stayDate;
        private final String currency;
        private final String amount;
    }
}
