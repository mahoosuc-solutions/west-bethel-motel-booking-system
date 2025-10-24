package com.westbethel.motel_booking.loyalty.service.impl;

import com.westbethel.motel_booking.loyalty.domain.LoyaltyProfile;
import com.westbethel.motel_booking.loyalty.domain.LoyaltyTier;
import com.westbethel.motel_booking.loyalty.model.LoyaltyAccrualRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltyRedemptionRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltySummary;
import com.westbethel.motel_booking.loyalty.repository.LoyaltyProfileRepository;
import com.westbethel.motel_booking.loyalty.service.LoyaltyService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultLoyaltyService implements LoyaltyService {

    private final LoyaltyProfileRepository loyaltyProfileRepository;

    public DefaultLoyaltyService(LoyaltyProfileRepository loyaltyProfileRepository) {
        this.loyaltyProfileRepository = loyaltyProfileRepository;
    }

    @Override
    public LoyaltySummary accrue(LoyaltyAccrualRequest request) {
        LoyaltyProfile profile = getOrCreateProfile(request.getGuestId());
        profile = profile.toBuilder()
                .pointsBalance(profile.getPointsBalance() + request.getPoints())
                .tier(recalculateTier(profile.getPointsBalance() + request.getPoints()))
                .updatedAt(OffsetDateTime.now())
                .build();
        loyaltyProfileRepository.save(profile);
        return toSummary(profile);
    }

    @Override
    public LoyaltySummary redeem(LoyaltyRedemptionRequest request) {
        LoyaltyProfile profile = getOrCreateProfile(request.getGuestId());
        long remaining = profile.getPointsBalance() - request.getPoints();
        if (remaining < 0) {
            throw new IllegalStateException("Insufficient loyalty points");
        }
        profile = profile.toBuilder()
                .pointsBalance(remaining)
                .tier(recalculateTier(remaining))
                .updatedAt(OffsetDateTime.now())
                .build();
        loyaltyProfileRepository.save(profile);
        return toSummary(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public LoyaltySummary getSummary(UUID guestId) {
        return loyaltyProfileRepository.findByGuestId(guestId)
                .map(this::toSummary)
                .orElse(LoyaltySummary.builder()
                        .tier(LoyaltyTier.STANDARD)
                        .pointsBalance(0L)
                        .pointsExpiringSoon(0L)
                        .build());
    }

    private LoyaltyProfile getOrCreateProfile(UUID guestId) {
        return loyaltyProfileRepository.findByGuestId(guestId)
                .orElseGet(() -> loyaltyProfileRepository.save(LoyaltyProfile.builder()
                        .id(UUID.randomUUID())
                        .guestId(guestId)
                        .tier(LoyaltyTier.STANDARD)
                        .pointsBalance(0L)
                        .updatedAt(OffsetDateTime.now())
                        .build()));
    }

    private LoyaltyTier recalculateTier(long points) {
        if (points >= 10000) {
            return LoyaltyTier.PLATINUM;
        } else if (points >= 5000) {
            return LoyaltyTier.GOLD;
        } else if (points >= 2000) {
            return LoyaltyTier.SILVER;
        }
        return LoyaltyTier.STANDARD;
    }

    private LoyaltySummary toSummary(LoyaltyProfile profile) {
        return LoyaltySummary.builder()
                .tier(profile.getTier())
                .pointsBalance(profile.getPointsBalance())
                .pointsExpiringSoon(0L)
                .build();
    }
}
