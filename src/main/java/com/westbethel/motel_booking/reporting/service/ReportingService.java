package com.westbethel.motel_booking.reporting.service;

import com.westbethel.motel_booking.reporting.model.ReportRequest;
import com.westbethel.motel_booking.reporting.model.ReportResult;

public interface ReportingService {

    ReportResult generate(ReportRequest request);
}
