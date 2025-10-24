package com.westbethel.motel_booking.property.repository;

import com.westbethel.motel_booking.property.domain.Property;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByCode(String code);
}
