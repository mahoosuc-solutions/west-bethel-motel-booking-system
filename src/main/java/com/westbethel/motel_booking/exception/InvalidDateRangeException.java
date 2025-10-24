package com.westbethel.motel_booking.exception;

import java.time.LocalDate;

/**
 * Exception thrown when an invalid date range is provided.
 */
public class InvalidDateRangeException extends BookingException {

    private final LocalDate startDate;
    private final LocalDate endDate;

    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super("INVALID_DATE_RANGE",
              String.format("Invalid date range: start date %s must be before end date %s", startDate, endDate));
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message);
        this.startDate = null;
        this.endDate = null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
