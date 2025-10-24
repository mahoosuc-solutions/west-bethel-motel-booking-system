package com.westbethel.motel_booking.inventory.api.mapper;

import com.westbethel.motel_booking.billing.api.mapper.PaymentMapper;
import com.westbethel.motel_booking.inventory.api.dto.RoomResponseDto;
import com.westbethel.motel_booking.inventory.api.dto.RoomTypeResponseDto;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    private final PaymentMapper paymentMapper;

    public InventoryMapper(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    public RoomResponseDto toDto(Room room) {
        return RoomResponseDto.builder()
                .id(room.getId())
                .propertyId(room.getPropertyId())
                .roomTypeId(room.getRoomTypeId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus())
                .housekeepingStatus(room.getHousekeepingStatus())
                .maintenanceNotes(room.getMaintenanceNotes())
                .build();
    }

    public RoomTypeResponseDto toDto(RoomType roomType) {
        return RoomTypeResponseDto.builder()
                .id(roomType.getId())
                .propertyId(roomType.getPropertyId())
                .code(roomType.getCode())
                .name(roomType.getName())
                .description(roomType.getDescription())
                .capacity(roomType.getCapacity())
                .bedConfiguration(roomType.getBedConfiguration())
                .amenities(roomType.getAmenities())
                .baseRate(roomType.getBaseRate() != null
                        ? paymentMapper.toAmountDto(roomType.getBaseRate())
                        : null)
                .build();
    }
}
