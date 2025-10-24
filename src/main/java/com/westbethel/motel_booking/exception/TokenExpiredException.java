package com.westbethel.motel_booking.exception;

/**
 * Exception thrown when an authentication token has expired.
 */
public class TokenExpiredException extends BookingException {

    public TokenExpiredException() {
        super("TOKEN_EXPIRED", "Authentication token has expired");
    }

    public TokenExpiredException(String message) {
        super("TOKEN_EXPIRED", message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super("TOKEN_EXPIRED", message, cause);
    }
}
