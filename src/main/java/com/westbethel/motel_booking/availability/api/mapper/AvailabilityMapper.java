package com.westbethel.motel_booking.availability.api.mapper;

import com.westbethel.motel_booking.availability.api.dto.AvailabilityQueryDto;
import com.westbethel.motel_booking.availability.api.dto.AvailabilityResultDto;
import com.westbethel.motel_booking.availability.model.AvailabilityQuery;
import com.westbethel.motel_booking.availability.model.AvailabilityResult;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityMapper {

    public AvailabilityQuery toQuery(AvailabilityQueryDto dto) {
        return AvailabilityQuery.builder()
                .propertyId(dto.getPropertyId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .adults(dto.getAdults())
                .children(dto.getChildren())
                .roomTypeCodes(dto.getRoomTypeCodes())
                .build();
    }

    public AvailabilityResultDto toDto(AvailabilityResult result) {
        return AvailabilityResultDto.builder()
                .roomTypes(result.getRoomTypes().stream()
                        .map(this::toRoomTypeDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private AvailabilityResultDto.RoomTypeAvailabilityDto toRoomTypeDto(
            AvailabilityResult.RoomTypeAvailability roomType) {
        return AvailabilityResultDto.RoomTypeAvailabilityDto.builder()
                .roomTypeCode(roomType.getRoomTypeCode())
                .roomTypeName(roomType.getRoomTypeCode()) // Could be enhanced with actual name
                .availableRooms(roomType.getAvailableRooms())
                .nightlyRates(roomType.getNightlyRates().stream()
                        .map(this::toNightlyRateDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private AvailabilityResultDto.NightlyRateDto toNightlyRateDto(
            AvailabilityResult.NightlyRate nightlyRate) {
        return AvailabilityResultDto.NightlyRateDto.builder()
                .stayDate(nightlyRate.getStayDate())
                .currency(nightlyRate.getCurrency())
                .amount(nightlyRate.getAmount())
                .build();
    }
}
