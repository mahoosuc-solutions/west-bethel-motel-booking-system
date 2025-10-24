package com.westbethel.motel_booking.loyalty.api;

import com.westbethel.motel_booking.loyalty.api.dto.LoyaltyPointsRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltyAccrualRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltyRedemptionRequest;
import com.westbethel.motel_booking.loyalty.model.LoyaltySummary;
import com.westbethel.motel_booking.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loyalty")
@Validated
@PreAuthorize("hasRole('USER')")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @PostMapping("/{guestId}/accrue")
    public ResponseEntity<LoyaltySummary> accrue(
            @PathVariable UUID guestId,
            @Valid @RequestBody LoyaltyPointsRequest payload) {
        LoyaltyAccrualRequest request = LoyaltyAccrualRequest.builder()
                .guestId(guestId)
                .bookingId(null)
                .points(payload.getPoints())
                .description(payload.getDescription())
                .build();
        return ResponseEntity.ok(loyaltyService.accrue(request));
    }

    @PostMapping("/{guestId}/redeem")
    public ResponseEntity<LoyaltySummary> redeem(
            @PathVariable UUID guestId,
            @Valid @RequestBody LoyaltyPointsRequest payload) {
        LoyaltyRedemptionRequest request = LoyaltyRedemptionRequest.builder()
                .guestId(guestId)
                .bookingId(null)
                .points(payload.getPoints())
                .reason(payload.getDescription())
                .build();
        return ResponseEntity.ok(loyaltyService.redeem(request));
    }

    @GetMapping("/{guestId}")
    public ResponseEntity<LoyaltySummary> summary(@PathVariable UUID guestId) {
        return ResponseEntity.ok(loyaltyService.getSummary(guestId));
    }
}
