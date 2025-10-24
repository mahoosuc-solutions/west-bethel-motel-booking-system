package com.westbethel.motel_booking.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when there are insufficient funds for a payment.
 */
public class InsufficientFundsException extends BookingException {

    private final BigDecimal requiredAmount;
    private final BigDecimal availableAmount;

    public InsufficientFundsException(BigDecimal requiredAmount, BigDecimal availableAmount) {
        super("INSUFFICIENT_FUNDS",
              String.format("Insufficient funds: required %s, available %s", requiredAmount, availableAmount));
        this.requiredAmount = requiredAmount;
        this.availableAmount = availableAmount;
    }

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
        this.requiredAmount = null;
        this.availableAmount = null;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public BigDecimal getAvailableAmount() {
        return availableAmount;
    }
}
