package com.westbethel.motel_booking.exception;

/**
 * Exception thrown when invalid credentials are provided.
 */
public class InvalidCredentialsException extends BookingException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password");
    }

    public InvalidCredentialsException(String message) {
        super("INVALID_CREDENTIALS", message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super("INVALID_CREDENTIALS", message, cause);
    }
}
