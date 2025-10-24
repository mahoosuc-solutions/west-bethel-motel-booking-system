package com.westbethel.motel_booking.notification.admin;

import com.westbethel.motel_booking.notification.email.EmailMessage;
import com.westbethel.motel_booking.notification.email.EmailService;
import com.westbethel.motel_booking.notification.email.Priority;
import com.westbethel.motel_booking.notification.queue.EmailQueueService;
import com.westbethel.motel_booking.notification.queue.QueuedEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing email queue and sending test emails.
 * Requires ADMIN role for access.
 */
@RestController
@RequestMapping("/api/v1/admin/emails")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class EmailAdminController {

    private final EmailQueueService queueService;
    private final EmailService emailService;

    /**
     * Gets current email queue status and statistics.
     */
    @GetMapping("/queue/status")
    public ResponseEntity<EmailQueueService.QueueStatistics> getQueueStatus() {
        EmailQueueService.QueueStatistics stats = queueService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets all queued emails (pending and retrying).
     */
    @GetMapping("/queue")
    public ResponseEntity<List<QueuedEmail>> getQueuedEmails() {
        List<QueuedEmail> queued = queueService.getQueuedEmails();
        return ResponseEntity.ok(queued);
    }

    /**
     * Gets all failed emails.
     */
    @GetMapping("/failed")
    public ResponseEntity<List<QueuedEmail>> getFailedEmails() {
        List<QueuedEmail> failed = queueService.getFailedEmails();
        return ResponseEntity.ok(failed);
    }

    /**
     * Gets a specific queued email by ID.
     */
    @GetMapping("/queue/{id}")
    public ResponseEntity<QueuedEmail> getQueuedEmail(@PathVariable String id) {
        QueuedEmail email = queueService.getQueuedEmail(id);
        if (email == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(email);
    }

    /**
     * Retries a specific failed email.
     */
    @PostMapping("/retry/{id}")
    public ResponseEntity<Map<String, String>> retryEmail(@PathVariable String id) {
        queueService.retryEmail(id);
        log.info("Admin triggered retry for email: {}", id);
        return ResponseEntity.ok(Map.of(
                "message", "Email queued for retry",
                "emailId", id
        ));
    }

    /**
     * Retries all failed emails.
     */
    @PostMapping("/retry-all")
    public ResponseEntity<Map<String, String>> retryAllFailed() {
        queueService.retryFailed();
        log.info("Admin triggered retry for all failed emails");
        return ResponseEntity.ok(Map.of(
                "message", "All failed emails queued for retry"
        ));
    }

    /**
     * Deletes a queued email.
     */
    @DeleteMapping("/queue/{id}")
    public ResponseEntity<Map<String, String>> deleteQueuedEmail(@PathVariable String id) {
        queueService.deleteQueuedEmail(id);
        log.info("Admin deleted queued email: {}", id);
        return ResponseEntity.ok(Map.of(
                "message", "Email deleted from queue",
                "emailId", id
        ));
    }

    /**
     * Sends a test email.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestBody TestEmailRequest request) {
        EmailMessage.EmailMessageBuilder builder = EmailMessage.builder()
                .to(request.getTo())
                .priority(Priority.LOW);

        if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
            // Template email
            builder.templateName(request.getTemplateName())
                    .templateVariables(request.getTemplateVariables() != null
                            ? request.getTemplateVariables()
                            : Map.of())
                    .subject(request.getSubject() != null ? request.getSubject() : "Test Email");
        } else {
            // Simple text email
            builder.subject(request.getSubject() != null ? request.getSubject() : "Test Email")
                    .body(request.getBody() != null ? request.getBody() : "This is a test email.");
        }

        EmailMessage message = builder.build();
        emailService.sendEmail(message);

        log.info("Admin sent test email to: {}", request.getTo());
        return ResponseEntity.ok(Map.of(
                "message", "Test email sent successfully",
                "recipient", request.getTo()
        ));
    }

    /**
     * Processes the email queue immediately (triggers scheduled job).
     */
    @PostMapping("/queue/process")
    public ResponseEntity<Map<String, String>> processQueue() {
        queueService.processQueue();
        log.info("Admin triggered manual queue processing");
        return ResponseEntity.ok(Map.of(
                "message", "Queue processing triggered"
        ));
    }
}
