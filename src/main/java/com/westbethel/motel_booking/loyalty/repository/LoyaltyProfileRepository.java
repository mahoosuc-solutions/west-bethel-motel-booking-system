package com.westbethel.motel_booking.loyalty.repository;

import com.westbethel.motel_booking.loyalty.domain.LoyaltyProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyProfileRepository extends JpaRepository<LoyaltyProfile, UUID> {

    Optional<LoyaltyProfile> findByGuestId(UUID guestId);
}
