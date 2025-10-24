package com.westbethel.motel_booking.pricing.model;

import com.westbethel.motel_booking.common.model.Money;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PricingQuote {

    private final Money baseAmount;
    private final Money taxAmount;
    private final Money totalAmount;
    private final List<Adjustment> adjustments;

    @Getter
    @Builder
    public static class Adjustment {
        private final String description;
        private final Money value;
    }
}
