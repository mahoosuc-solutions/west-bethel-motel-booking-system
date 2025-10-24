package com.westbethel.motel_booking.notification.queue;

import com.westbethel.motel_booking.notification.email.EmailMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Represents an email queued for sending in Redis.
 * Includes retry logic and tracking information.
 */
@RedisHash("email_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuedEmail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the queued email.
     */
    @Id
    private String id;

    /**
     * The email message to be sent.
     */
    private EmailMessage message;

    /**
     * Number of send attempts made.
     */
    @Builder.Default
    private int attemptCount = 0;

    /**
     * Maximum number of send attempts allowed.
     */
    @Builder.Default
    private int maxAttempts = 5;

    /**
     * Timestamp when the email was first queued.
     */
    private OffsetDateTime queuedAt;

    /**
     * Timestamp of the last send attempt.
     */
    private OffsetDateTime lastAttemptAt;

    /**
     * Timestamp when the email should be retried (for exponential backoff).
     */
    private OffsetDateTime retryAt;

    /**
     * Current status of the email.
     */
    @Builder.Default
    private EmailStatus status = EmailStatus.QUEUED;

    /**
     * Error message from the last failed attempt.
     */
    private String errorMessage;

    /**
     * Stack trace from the last failed attempt (for debugging).
     */
    private String errorStackTrace;

    /**
     * Time-to-live in seconds (7 days = 604800 seconds).
     * After this period, the record will be automatically removed from Redis.
     */
    @TimeToLive
    @Builder.Default
    private Long ttl = 604800L; // 7 days

    /**
     * Checks if the email can be retried.
     */
    public boolean canRetry() {
        return attemptCount < maxAttempts && status != EmailStatus.SENT;
    }

    /**
     * Checks if the email is ready to be retried (retry time has passed).
     */
    public boolean isReadyForRetry() {
        if (retryAt == null) {
            return true;
        }
        return OffsetDateTime.now().isAfter(retryAt);
    }

    /**
     * Increments the attempt count.
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
        this.lastAttemptAt = OffsetDateTime.now();
    }

    /**
     * Marks the email as sent successfully.
     */
    public void markAsSent() {
        this.status = EmailStatus.SENT;
        this.lastAttemptAt = OffsetDateTime.now();
    }

    /**
     * Marks the email as failed.
     */
    public void markAsFailed(String errorMessage, String stackTrace) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStackTrace = stackTrace;
        this.lastAttemptAt = OffsetDateTime.now();
    }

    /**
     * Calculates the next retry time using exponential backoff.
     * Attempt 1: Immediate
     * Attempt 2: After 1 minute
     * Attempt 3: After 5 minutes
     * Attempt 4: After 15 minutes
     * Attempt 5: After 1 hour
     */
    public void calculateNextRetryTime() {
        OffsetDateTime now = OffsetDateTime.now();
        switch (attemptCount) {
            case 0:
                this.retryAt = now; // Immediate
                break;
            case 1:
                this.retryAt = now.plusMinutes(1);
                break;
            case 2:
                this.retryAt = now.plusMinutes(5);
                break;
            case 3:
                this.retryAt = now.plusMinutes(15);
                break;
            case 4:
                this.retryAt = now.plusHours(1);
                break;
            default:
                this.retryAt = now.plusHours(2);
        }
    }
}
