package com.westbethel.motel_booking.availability.api;

import com.westbethel.motel_booking.availability.model.AvailabilityQuery;
import com.westbethel.motel_booking.availability.model.AvailabilityResult;
import com.westbethel.motel_booking.availability.service.AvailabilityService;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<AvailabilityResult> searchAvailability(
            @RequestParam("propertyId") UUID propertyId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "adults", defaultValue = "1") Integer adults,
            @RequestParam(value = "children", defaultValue = "0") Integer children,
            @RequestParam(value = "roomTypes", required = false) Set<String> roomTypes) {

        AvailabilityQuery query = AvailabilityQuery.builder()
                .propertyId(propertyId)
                .startDate(startDate)
                .endDate(endDate)
                .adults(adults)
                .children(children)
                .roomTypeCodes(roomTypes)
                .build();

        AvailabilityResult result = availabilityService.searchAvailability(query);
        return ResponseEntity.ok(result);
    }
}
