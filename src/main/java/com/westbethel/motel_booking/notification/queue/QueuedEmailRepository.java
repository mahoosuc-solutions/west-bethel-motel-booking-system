package com.westbethel.motel_booking.notification.queue;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing queued emails in Redis.
 */
@Repository
public interface QueuedEmailRepository extends CrudRepository<QueuedEmail, String> {

    /**
     * Finds all emails with a specific status.
     */
    List<QueuedEmail> findByStatus(EmailStatus status);

    /**
     * Finds all emails that are queued or retrying.
     */
    default List<QueuedEmail> findPendingEmails() {
        List<QueuedEmail> queued = findByStatus(EmailStatus.QUEUED);
        List<QueuedEmail> retrying = findByStatus(EmailStatus.RETRYING);
        queued.addAll(retrying);
        return queued;
    }

    /**
     * Finds all failed emails.
     */
    default List<QueuedEmail> findFailedEmails() {
        return findByStatus(EmailStatus.FAILED);
    }
}
