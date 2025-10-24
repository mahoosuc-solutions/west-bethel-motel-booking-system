package com.westbethel.motel_booking.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loyalty_profiles")
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class LoyaltyProfile {

    @Id
    private UUID id;

    @Column(name = "guest_id", nullable = false, unique = true)
    private UUID guestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 32, nullable = false)
    private LoyaltyTier tier;

    @Column(name = "points_balance", nullable = false)
    private Long pointsBalance;

    @Column(name = "points_expiry", length = 1024)
    private String pointsExpiryPolicy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
