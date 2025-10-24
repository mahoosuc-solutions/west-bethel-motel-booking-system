package com.westbethel.motel_booking.loyalty.service;

import com.westbethel.motel_booking.loyalty.model.LoyaltyAccrualRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltyRedemptionRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltySummary;
import java.util.UUID;

public interface LoyaltyService {

    LoyaltySummary accrue(LoyaltyAccrualRequest request);

    LoyaltySummary redeem(LoyaltyRedemptionRequest request);

    LoyaltySummary getSummary(UUID guestId);
}
