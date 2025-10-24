package com.westbethel.motel_booking.inventory.repository;

import com.westbethel.motel_booking.common.model.RoomStatus;
import com.westbethel.motel_booking.inventory.domain.Room;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByPropertyId(UUID propertyId);

    List<Room> findByPropertyIdAndRoomTypeId(UUID propertyId, UUID roomTypeId);

    List<Room> findByPropertyIdAndRoomTypeIdAndStatus(UUID propertyId, UUID roomTypeId, RoomStatus status);
}
