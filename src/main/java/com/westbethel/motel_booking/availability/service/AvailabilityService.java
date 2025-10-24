package com.westbethel.motel_booking.availability.service;

import com.westbethel.motel_booking.availability.model.AvailabilityQuery;
import com.westbethel.motel_booking.availability.model.AvailabilityResult;

public interface AvailabilityService {

    AvailabilityResult searchAvailability(AvailabilityQuery query);
}
