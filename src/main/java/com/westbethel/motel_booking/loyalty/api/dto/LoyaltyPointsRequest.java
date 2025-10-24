package com.westbethel.motel_booking.loyalty.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoyaltyPointsRequest {

    @NotNull
    @Min(0)
    private Long points;

    private String description;
}
