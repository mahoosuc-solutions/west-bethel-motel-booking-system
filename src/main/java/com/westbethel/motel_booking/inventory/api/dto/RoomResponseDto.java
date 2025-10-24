package com.westbethel.motel_booking.inventory.api.dto;

import com.westbethel.motel_booking.common.model.HousekeepingStatus;
import com.westbethel.motel_booking.common.model.RoomStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomResponseDto {

    private final UUID id;
    private final UUID propertyId;
    private final UUID roomTypeId;
    private final String roomNumber;
    private final String floor;
    private final RoomStatus status;
    private final HousekeepingStatus housekeepingStatus;
    private final String maintenanceNotes;
}
