package com.westbethel.motel_booking.inventory.api.dto;

import com.westbethel.motel_booking.billing.api.dto.PaymentAmountDto;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomTypeResponseDto {

    private final UUID id;
    private final UUID propertyId;
    private final String code;
    private final String name;
    private final String description;
    private final Integer capacity;
    private final String bedConfiguration;
    private final Set<String> amenities;
    private final PaymentAmountDto baseRate;
}
