package com.westbethel.motel_booking.loyalty.model;

import com.westbethel.motel_booking.loyalty.domain.LoyaltyTier;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoyaltySummary {

    private final LoyaltyTier tier;
    private final Long pointsBalance;
    private final Long pointsExpiringSoon;
}
