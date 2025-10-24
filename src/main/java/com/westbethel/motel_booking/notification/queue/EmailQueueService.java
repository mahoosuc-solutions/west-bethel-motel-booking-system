package com.westbethel.motel_booking.notification.queue;

import com.westbethel.motel_booking.notification.email.EmailMessage;
import com.westbethel.motel_booking.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing email queue with retry logic and exponential backoff.
 * Processes queued emails on a scheduled basis.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailQueueService {

    private final QueuedEmailRepository repository;
    private final EmailService emailService;

    @Value("${notification.queue.enabled:true}")
    private boolean queueEnabled;

    @Value("${notification.queue.max-retries:5}")
    private int maxRetries;

    /**
     * Adds an email to the queue for sending.
     *
     * @param message the email message to queue
     * @return the queued email ID
     */
    public String enqueue(EmailMessage message) {
        if (!queueEnabled) {
            // If queue is disabled, send immediately
            emailService.sendEmail(message);
            return null;
        }

        QueuedEmail queuedEmail = QueuedEmail.builder()
                .id(UUID.randomUUID().toString())
                .message(message)
                .attemptCount(0)
                .maxAttempts(maxRetries)
                .queuedAt(OffsetDateTime.now())
                .status(EmailStatus.QUEUED)
                .build();

        queuedEmail.calculateNextRetryTime();
        repository.save(queuedEmail);

        log.info("Email queued for sending: {} (ID: {})", message.getTo(), queuedEmail.getId());
        return queuedEmail.getId();
    }

    /**
     * Processes the email queue on a scheduled basis (every 10 seconds).
     * Sends queued emails and handles retries with exponential backoff.
     */
    @Scheduled(fixedDelay = 10000) // Every 10 seconds
    public void processQueue() {
        if (!queueEnabled) {
            return;
        }

        try {
            List<QueuedEmail> pendingEmails = repository.findPendingEmails()
                    .stream()
                    .filter(QueuedEmail::isReadyForRetry)
                    .collect(Collectors.toList());

            if (pendingEmails.isEmpty()) {
                return;
            }

            log.debug("Processing {} queued emails", pendingEmails.size());

            for (QueuedEmail queuedEmail : pendingEmails) {
                processQueuedEmail(queuedEmail);
            }
        } catch (Exception e) {
            log.error("Error processing email queue", e);
        }
    }

    /**
     * Processes a single queued email.
     */
    private void processQueuedEmail(QueuedEmail queuedEmail) {
        try {
            queuedEmail.setStatus(EmailStatus.SENDING);
            queuedEmail.incrementAttemptCount();
            repository.save(queuedEmail);

            // Attempt to send the email
            emailService.sendEmail(queuedEmail.getMessage());

            // Mark as sent on success
            queuedEmail.markAsSent();
            repository.save(queuedEmail);

            log.info("Successfully sent queued email: {} (Attempt {}/{})",
                    queuedEmail.getId(),
                    queuedEmail.getAttemptCount(),
                    queuedEmail.getMaxAttempts());

        } catch (Exception e) {
            handleSendingFailure(queuedEmail, e);
        }
    }

    /**
     * Handles email sending failure with retry logic.
     */
    private void handleSendingFailure(QueuedEmail queuedEmail, Exception e) {
        log.error("Failed to send queued email: {} (Attempt {}/{})",
                queuedEmail.getId(),
                queuedEmail.getAttemptCount(),
                queuedEmail.getMaxAttempts(),
                e);

        if (queuedEmail.canRetry()) {
            // Calculate next retry time and update status
            queuedEmail.setStatus(EmailStatus.RETRYING);
            queuedEmail.calculateNextRetryTime();
            queuedEmail.setErrorMessage(e.getMessage());
            queuedEmail.setErrorStackTrace(getStackTrace(e));

            repository.save(queuedEmail);

            log.info("Email {} will be retried at {}",
                    queuedEmail.getId(),
                    queuedEmail.getRetryAt());
        } else {
            // Max retries exceeded, mark as failed
            queuedEmail.markAsFailed(e.getMessage(), getStackTrace(e));
            repository.save(queuedEmail);

            log.error("Email {} failed after {} attempts and will not be retried",
                    queuedEmail.getId(),
                    queuedEmail.getAttemptCount());
        }
    }

    /**
     * Retries all failed emails.
     */
    public void retryFailed() {
        List<QueuedEmail> failedEmails = repository.findFailedEmails();

        log.info("Retrying {} failed emails", failedEmails.size());

        for (QueuedEmail email : failedEmails) {
            // Reset for retry
            email.setStatus(EmailStatus.QUEUED);
            email.setAttemptCount(0);
            email.setErrorMessage(null);
            email.setErrorStackTrace(null);
            email.calculateNextRetryTime();
            repository.save(email);
        }
    }

    /**
     * Retries a specific failed email by ID.
     */
    public void retryEmail(String emailId) {
        repository.findById(emailId).ifPresent(email -> {
            if (email.getStatus() == EmailStatus.FAILED) {
                email.setStatus(EmailStatus.QUEUED);
                email.setAttemptCount(0);
                email.setErrorMessage(null);
                email.setErrorStackTrace(null);
                email.calculateNextRetryTime();
                repository.save(email);

                log.info("Email {} reset for retry", emailId);
            }
        });
    }

    /**
     * Gets all failed emails.
     */
    public List<QueuedEmail> getFailedEmails() {
        return repository.findFailedEmails();
    }

    /**
     * Gets all queued emails.
     */
    public List<QueuedEmail> getQueuedEmails() {
        return repository.findPendingEmails();
    }

    /**
     * Gets a specific queued email by ID.
     */
    public QueuedEmail getQueuedEmail(String id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * Deletes a queued email by ID.
     */
    public void deleteQueuedEmail(String id) {
        repository.deleteById(id);
        log.info("Deleted queued email: {}", id);
    }

    /**
     * Gets the stack trace from an exception as a string.
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Gets queue statistics.
     */
    public QueueStatistics getStatistics() {
        List<QueuedEmail> all = (List<QueuedEmail>) repository.findAll();

        long queued = all.stream().filter(e -> e.getStatus() == EmailStatus.QUEUED).count();
        long sending = all.stream().filter(e -> e.getStatus() == EmailStatus.SENDING).count();
        long sent = all.stream().filter(e -> e.getStatus() == EmailStatus.SENT).count();
        long failed = all.stream().filter(e -> e.getStatus() == EmailStatus.FAILED).count();
        long retrying = all.stream().filter(e -> e.getStatus() == EmailStatus.RETRYING).count();

        return new QueueStatistics(queued, sending, sent, failed, retrying, all.size());
    }

    /**
     * Queue statistics DTO.
     */
    public record QueueStatistics(
            long queued,
            long sending,
            long sent,
            long failed,
            long retrying,
            long total
    ) {
    }
}
