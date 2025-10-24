package com.westbethel.motel_booking.loyalty.api.mapper;

import com.westbethel.motel_booking.loyalty.api.dto.LoyaltyPointsRequest;
import com.westbethel.motel_booking.loyalty.api.dto.LoyaltySummaryDto;
import com.westbethel.motel_booking.loyalty.model.LoyaltyAccrualRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltyRedemptionRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltySummary;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyMapper {

    public LoyaltyAccrualRequest toAccrualRequest(UUID guestId, LoyaltyPointsRequest dto) {
        return LoyaltyAccrualRequest.builder()
                .guestId(guestId)
                .bookingId(null)
                .points(dto.getPoints())
                .description(dto.getDescription())
                .build();
    }

    public LoyaltyRedemptionRequest toRedemptionRequest(UUID guestId, LoyaltyPointsRequest dto) {
        return LoyaltyRedemptionRequest.builder()
                .guestId(guestId)
                .bookingId(null)
                .points(dto.getPoints())
                .reason(dto.getDescription())
                .build();
    }

    public LoyaltySummaryDto toDto(LoyaltySummary summary) {
        return LoyaltySummaryDto.builder()
                .tier(summary.getTier() != null ? summary.getTier().name() : null)
                .pointsBalance(summary.getPointsBalance())
                .pointsExpiringSoon(summary.getPointsExpiringSoon())
                .build();
    }
}
