package com.westbethel.motel_booking.notification.queue;

/**
 * Status of queued emails in the system.
 */
public enum EmailStatus {
    /**
     * Email is queued and waiting to be sent.
     */
    QUEUED,

    /**
     * Email is currently being sent.
     */
    SENDING,

    /**
     * Email has been successfully sent.
     */
    SENT,

    /**
     * Email sending failed after all retry attempts.
     */
    FAILED,

    /**
     * Email sending is being retried.
     */
    RETRYING
}
