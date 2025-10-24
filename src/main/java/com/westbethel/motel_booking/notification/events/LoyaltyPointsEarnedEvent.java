package com.westbethel.motel_booking.notification.events;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Event published when a user earns loyalty points.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoyaltyPointsEarnedEvent extends NotificationEvent {

    private String firstName;
    private Integer pointsEarned;
    private Integer totalPoints;
    private Integer previousBalance;
    private String bookingReference;
    private OffsetDateTime earnedAt;
    private Integer multiplier;
    private Integer pointsToNextTier;

    @Builder
    public LoyaltyPointsEarnedEvent(String userId, String email, String firstName,
                                   Integer pointsEarned, Integer totalPoints,
                                   Integer previousBalance, String bookingReference,
                                   Integer multiplier, Integer pointsToNextTier) {
        super();
        this.setUserId(userId);
        this.setEmail(email);
        this.firstName = firstName;
        this.pointsEarned = pointsEarned;
        this.totalPoints = totalPoints;
        this.previousBalance = previousBalance;
        this.bookingReference = bookingReference;
        this.earnedAt = OffsetDateTime.now();
        this.multiplier = multiplier != null ? multiplier : 1;
        this.pointsToNextTier = pointsToNextTier;
    }

    @Override
    public String getTemplateName() {
        return "loyalty-points-earned";
    }
}
