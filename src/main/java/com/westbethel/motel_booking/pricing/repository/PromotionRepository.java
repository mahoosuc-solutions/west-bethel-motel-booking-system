package com.westbethel.motel_booking.pricing.repository;

import com.westbethel.motel_booking.pricing.domain.Promotion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    List<Promotion> findByPropertyId(UUID propertyId);

    Optional<Promotion> findByPropertyIdAndCode(UUID propertyId, String code);

    @Query("""
            select p from Promotion p
            where p.propertyId = :propertyId
              and p.startsOn <= :date
              and (p.endsOn is null or p.endsOn >= :date)
            """)
    List<Promotion> findActivePromotions(
            @Param("propertyId") UUID propertyId,
            @Param("date") LocalDate date);
}
