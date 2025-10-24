package com.westbethel.motel_booking.pricing.service.impl;

import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.inventory.repository.RoomTypeRepository;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.pricing.model.PricingContext;
import com.westbethel.motel_booking.pricing.model.PricingQuote;
import com.westbethel.motel_booking.pricing.repository.RatePlanRepository;
import com.westbethel.motel_booking.pricing.service.PricingService;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.property.repository.PropertyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultPricingService implements PricingService {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RatePlanRepository ratePlanRepository;

    public DefaultPricingService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RatePlanRepository ratePlanRepository) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Override
    public PricingQuote quote(PricingContext context) {
        if (context.getCheckIn() == null || context.getCheckOut() == null) {
            throw new IllegalArgumentException("Stay dates are required for pricing");
        }
        long nights = ChronoUnit.DAYS.between(context.getCheckIn(), context.getCheckOut());
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in for pricing");
        }
        if (context.getRoomTypeIds() == null || context.getRoomTypeIds().isEmpty()) {
            throw new IllegalArgumentException("Room types are required for pricing");
        }

        Property property = propertyRepository.findById(context.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        RatePlan ratePlan = ratePlanRepository.findByPropertyIdAndId(property.getId(), context.getRatePlanId())
                .orElseThrow(() -> new IllegalArgumentException("Rate plan not found for property"));

        Currency currency = property.getDefaultCurrency();
        BigDecimal baseTotal = BigDecimal.ZERO;
        for (UUID roomTypeId : context.getRoomTypeIds()) {
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Room type not found"));

            Money baseRate = roomType.getBaseRate() != null ? roomType.getBaseRate() : ratePlan.getDefaultRate();
            BigDecimal nightly = baseRate != null && baseRate.getAmount() != null
                    ? baseRate.getAmount()
                    : BigDecimal.ZERO;
            if (baseRate != null && baseRate.getCurrency() != null) {
                currency = baseRate.getCurrency();
            }
            baseTotal = baseTotal.add(nightly.multiply(BigDecimal.valueOf(nights)));
        }

        baseTotal = baseTotal.setScale(2, RoundingMode.HALF_UP);
        Money baseAmount = Money.builder()
                .amount(baseTotal)
                .currency(currency)
                .build();

        Money taxAmount = Money.builder()
                .amount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .currency(currency)
                .build();

        Money totalAmount = Money.builder()
                .amount(baseAmount.getAmount().add(taxAmount.getAmount()))
                .currency(currency)
                .build();

        return PricingQuote.builder()
                .baseAmount(baseAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .adjustments(Collections.emptyList())
                .build();
    }
}
