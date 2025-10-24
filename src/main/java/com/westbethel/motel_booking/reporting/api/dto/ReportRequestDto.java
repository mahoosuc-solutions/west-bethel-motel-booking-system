package com.westbethel.motel_booking.reporting.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDto {

    public enum ReportType {
        OCCUPANCY,
        REVENUE,
        GUEST_STATS
    }

    @NotNull(message = "Report type is required")
    private ReportType type;

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;
}
