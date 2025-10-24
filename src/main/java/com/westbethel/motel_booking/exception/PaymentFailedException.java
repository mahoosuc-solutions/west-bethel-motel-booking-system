package com.westbethel.motel_booking.exception;

/**
 * Exception thrown when a payment operation fails.
 */
public class PaymentFailedException extends BookingException {

    private final String paymentId;
    private final String reason;

    public PaymentFailedException(String paymentId, String reason) {
        super("PAYMENT_FAILED",
              String.format("Payment %s failed: %s", paymentId, reason));
        this.paymentId = paymentId;
        this.reason = reason;
    }

    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message);
        this.paymentId = null;
        this.reason = null;
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, cause);
        this.paymentId = null;
        this.reason = null;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getReason() {
        return reason;
    }
}
