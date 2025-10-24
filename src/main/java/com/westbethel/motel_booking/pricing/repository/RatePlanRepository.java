package com.westbethel.motel_booking.pricing.repository;

import com.westbethel.motel_booking.pricing.domain.RatePlan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePlanRepository extends JpaRepository<RatePlan, UUID> {

    Optional<RatePlan> findByPropertyIdAndId(UUID propertyId, UUID id);
}
