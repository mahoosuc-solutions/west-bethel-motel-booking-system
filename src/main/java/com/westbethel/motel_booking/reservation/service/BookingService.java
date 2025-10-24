package com.westbethel.motel_booking.reservation.service;

import com.westbethel.motel_booking.pricing.model.PricingQuote;
import com.westbethel.motel_booking.reservation.model.BookingRequest;
import com.westbethel.motel_booking.reservation.model.BookingResponse;
import com.westbethel.motel_booking.reservation.model.CancellationRequest;

public interface BookingService {

    PricingQuote quote(BookingRequest request);

    BookingResponse create(BookingRequest request);

    BookingResponse amend(String confirmationNumber, BookingRequest request);

    BookingResponse cancel(CancellationRequest request);
}
