package com.westbethel.motel_booking.exception;

/**
 * Base exception for all booking-related exceptions.
 * Provides common functionality for error handling across the application.
 */
public class BookingException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public BookingException(String message) {
        super(message);
        this.errorCode = "BOOKING_ERROR";
        this.args = new Object[0];
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BOOKING_ERROR";
        this.args = new Object[0];
    }

    public BookingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BookingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BookingException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
