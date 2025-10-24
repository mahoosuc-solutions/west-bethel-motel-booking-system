package com.westbethel.motel_booking.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
public class UnauthorizedException extends BookingException {

    public UnauthorizedException() {
        super("UNAUTHORIZED", "You are not authorized to perform this action");
    }

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super("UNAUTHORIZED", message, cause);
    }
}
