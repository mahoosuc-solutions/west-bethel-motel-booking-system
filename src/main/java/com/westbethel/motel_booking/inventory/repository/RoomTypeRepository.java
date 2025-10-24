package com.westbethel.motel_booking.inventory.repository;

import com.westbethel.motel_booking.inventory.domain.RoomType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByPropertyId(UUID propertyId);

    List<RoomType> findByPropertyIdAndCodeIn(UUID propertyId, Collection<String> codes);

    Optional<RoomType> findByPropertyIdAndCode(UUID propertyId, String code);
}
