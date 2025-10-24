package com.westbethel.motel_booking.loyalty.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoyaltySummaryDto {

    private final String tier;
    private final Long pointsBalance;
    private final Long pointsExpiringSoon;
}
