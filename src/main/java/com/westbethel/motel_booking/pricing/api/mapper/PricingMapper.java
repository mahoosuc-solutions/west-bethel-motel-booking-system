package com.westbethel.motel_booking.pricing.api.mapper;

import com.westbethel.motel_booking.billing.api.dto.PaymentAmountDto;
import com.westbethel.motel_booking.billing.api.mapper.PaymentMapper;
import com.westbethel.motel_booking.pricing.api.dto.PricingContextDto;
import com.westbethel.motel_booking.pricing.api.dto.PricingQuoteDto;
import com.westbethel.motel_booking.pricing.model.PricingContext;
import com.westbethel.motel_booking.pricing.model.PricingQuote;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PricingMapper {

    private final PaymentMapper paymentMapper;

    public PricingMapper(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    public PricingContext toContext(PricingContextDto dto) {
        return PricingContext.builder()
                .propertyId(dto.getPropertyId())
                .ratePlanId(dto.getRatePlanId())
                .checkIn(dto.getCheckIn())
                .checkOut(dto.getCheckOut())
                .adults(dto.getAdults())
                .children(dto.getChildren())
                .guestId(dto.getGuestId())
                .roomTypeIds(dto.getRoomTypeIds())
                .build();
    }

    public PricingQuoteDto toDto(PricingQuote quote) {
        return PricingQuoteDto.builder()
                .baseAmount(paymentMapper.toAmountDto(quote.getBaseAmount()))
                .taxAmount(paymentMapper.toAmountDto(quote.getTaxAmount()))
                .totalAmount(paymentMapper.toAmountDto(quote.getTotalAmount()))
                .adjustments(quote.getAdjustments() != null
                        ? quote.getAdjustments().stream()
                                .map(this::toAdjustmentDto)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    private PricingQuoteDto.AdjustmentDto toAdjustmentDto(PricingQuote.Adjustment adjustment) {
        return PricingQuoteDto.AdjustmentDto.builder()
                .description(adjustment.getDescription())
                .value(paymentMapper.toAmountDto(adjustment.getValue()))
                .build();
    }
}
