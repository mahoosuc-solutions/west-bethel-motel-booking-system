package com.westbethel.motel_booking.reporting.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportResult {

    private final String reportId;
    private final OffsetDateTime generatedAt;
    private final String format;
    private final byte[] payload;
}
