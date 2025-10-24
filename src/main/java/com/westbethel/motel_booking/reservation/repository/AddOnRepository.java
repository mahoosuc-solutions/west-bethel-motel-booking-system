package com.westbethel.motel_booking.reservation.repository;

import com.westbethel.motel_booking.reservation.domain.AddOn;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddOnRepository extends JpaRepository<AddOn, UUID> {

    List<AddOn> findByPropertyId(UUID propertyId);
}
