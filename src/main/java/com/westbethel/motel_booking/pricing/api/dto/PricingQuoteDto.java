package com.westbethel.motel_booking.pricing.api.dto;

import com.westbethel.motel_booking.billing.api.dto.PaymentAmountDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PricingQuoteDto {

    private final PaymentAmountDto baseAmount;
    private final PaymentAmountDto taxAmount;
    private final PaymentAmountDto totalAmount;
    private final List<AdjustmentDto> adjustments;

    @Getter
    @Builder
    public static class AdjustmentDto {
        private final String description;
        private final PaymentAmountDto value;
    }
}
