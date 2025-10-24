package com.westbethel.motel_booking.reporting.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportRequest {

    public enum ReportType {
        DAILY_OCCUPANCY,
        REVENUE_SUMMARY,
        ADR_TREND,
        HOUSEKEEPING_ROSTER,
        LOYALTY_ACTIVITY
    }

    private final ReportType type;
    private final UUID propertyId;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Map<String, Object> parameters;
}
