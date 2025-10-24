package com.westbethel.motel_booking.reporting.api;

import com.westbethel.motel_booking.reporting.model.ReportRequest;
import com.westbethel.motel_booking.reporting.model.ReportResult;
import com.westbethel.motel_booking.reporting.service.ReportingService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping
    public ResponseEntity<byte[]> generateReport(
            @RequestParam ReportRequest.ReportType type,
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        ReportRequest request = ReportRequest.builder()
                .type(type)
                .propertyId(propertyId)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        ReportResult result = reportingService.generate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + result.getReportId() + ".csv")
                .contentType(MediaType.parseMediaType(result.getFormat()))
                .body(result.getPayload());
    }
}
