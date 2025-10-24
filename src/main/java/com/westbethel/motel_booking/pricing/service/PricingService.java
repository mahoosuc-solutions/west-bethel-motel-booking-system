package com.westbethel.motel_booking.pricing.service;

import com.westbethel.motel_booking.pricing.model.PricingContext;
import com.westbethel.motel_booking.pricing.model.PricingQuote;

public interface PricingService {

    PricingQuote quote(PricingContext context);
}
