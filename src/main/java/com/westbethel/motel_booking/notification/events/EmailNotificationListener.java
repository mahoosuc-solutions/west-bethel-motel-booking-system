package com.westbethel.motel_booking.notification.events;

import com.westbethel.motel_booking.notification.email.EmailMessage;
import com.westbethel.motel_booking.notification.email.Priority;
import com.westbethel.motel_booking.notification.queue.EmailQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for notification events and sends corresponding emails.
 * All event handlers are async to avoid blocking the main application thread.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailQueueService emailQueueService;

    /**
     * Handles user registration events.
     */
    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Handling UserRegisteredEvent for user: {}", event.getEmail());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("verificationLink", event.getVerificationLink());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Welcome to West Bethel Motel!")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles email verification requested events.
     */
    @Async
    @EventListener
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Handling EmailVerificationRequestedEvent for user: {}", event.getEmail());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("verificationLink", event.getVerificationLink());
        variables.put("verificationCode", event.getVerificationCode());
        variables.put("expiryHours", event.getExpiryHours());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Verify Your Email Address")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles password reset requested events.
     */
    @Async
    @EventListener
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Handling PasswordResetRequestedEvent for user: {}", event.getEmail());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("resetLink", event.getResetLink());
        variables.put("expiryMinutes", event.getExpiryMinutes());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Reset Your Password")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.URGENT)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles password changed events.
     */
    @Async
    @EventListener
    public void handlePasswordChanged(PasswordChangedEvent event) {
        log.info("Handling PasswordChangedEvent for user: {}", event.getEmail());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("changedAt", event.getChangedAt());
        variables.put("ipAddress", event.getIpAddress());
        variables.put("device", event.getDevice());
        variables.put("location", event.getLocation());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Your Password Was Changed")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles booking created events.
     */
    @Async
    @EventListener
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Handling BookingCreatedEvent for booking: {}", event.getConfirmationNumber());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("confirmationNumber", event.getConfirmationNumber());
        variables.put("roomType", event.getRoomType());
        variables.put("checkInDate", event.getCheckInDate());
        variables.put("checkOutDate", event.getCheckOutDate());
        variables.put("numberOfNights", event.getNumberOfNights());
        variables.put("numberOfGuests", event.getNumberOfGuests());
        variables.put("specialRequests", event.getSpecialRequests());
        variables.put("totalAmount", event.getTotalAmount());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Booking Confirmation - " + event.getConfirmationNumber())
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles booking cancelled events.
     */
    @Async
    @EventListener
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Handling BookingCancelledEvent for booking: {}", event.getConfirmationNumber());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("confirmationNumber", event.getConfirmationNumber());
        variables.put("roomType", event.getRoomType());
        variables.put("checkInDate", event.getCheckInDate());
        variables.put("checkOutDate", event.getCheckOutDate());
        variables.put("cancelledAt", event.getCancelledAt());
        variables.put("refundAmount", event.getRefundAmount());
        variables.put("refundReference", event.getRefundReference());
        variables.put("cancellationFee", event.getCancellationFee());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Booking Cancellation - " + event.getConfirmationNumber())
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.NORMAL)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles payment received events.
     */
    @Async
    @EventListener
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("Handling PaymentReceivedEvent for receipt: {}", event.getReceiptNumber());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("receiptNumber", event.getReceiptNumber());
        variables.put("transactionDate", event.getTransactionDate());
        variables.put("paymentMethod", event.getPaymentMethod());
        variables.put("bookingReference", event.getBookingReference());
        variables.put("description", event.getDescription());
        variables.put("amount", event.getAmount());
        variables.put("transactionId", event.getTransactionId());
        variables.put("loyaltyPointsEarned", event.getLoyaltyPointsEarned());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Payment Receipt - " + event.getReceiptNumber())
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles payment failed events.
     */
    @Async
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Handling PaymentFailedEvent for booking: {}", event.getBookingReference());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("bookingReference", event.getBookingReference());
        variables.put("amount", event.getAmount());
        variables.put("paymentMethod", event.getPaymentMethod());
        variables.put("attemptedAt", event.getAttemptedAt());
        variables.put("failureReason", event.getFailureReason());
        variables.put("retryPaymentLink", event.getRetryPaymentLink());
        variables.put("hoursUntilCancellation", event.getHoursUntilCancellation());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("Payment Issue - Action Required")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.URGENT)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles loyalty points earned events.
     */
    @Async
    @EventListener
    public void handleLoyaltyPointsEarned(LoyaltyPointsEarnedEvent event) {
        log.info("Handling LoyaltyPointsEarnedEvent for user: {} ({} points)",
                event.getEmail(), event.getPointsEarned());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("pointsEarned", event.getPointsEarned());
        variables.put("totalPoints", event.getTotalPoints());
        variables.put("previousBalance", event.getPreviousBalance());
        variables.put("bookingReference", event.getBookingReference());
        variables.put("earnedAt", event.getEarnedAt());
        variables.put("multiplier", event.getMultiplier());
        variables.put("pointsToNextTier", event.getPointsToNextTier());

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject("You Earned " + event.getPointsEarned() + " Loyalty Points!")
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(Priority.NORMAL)
                .build();

        emailQueueService.enqueue(message);
    }

    /**
     * Handles security alert events.
     */
    @Async
    @EventListener
    public void handleSecurityAlert(SecurityAlertEvent event) {
        log.info("Handling SecurityAlertEvent for user: {} (Type: {})",
                event.getEmail(), event.getAlertType());

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", event.getFirstName());
        variables.put("alertType", event.getAlertType());
        variables.put("alertMessage", event.getAlertMessage());
        variables.put("isCritical", event.isCritical());
        variables.put("occurredAt", event.getOccurredAt());
        variables.put("ipAddress", event.getIpAddress());
        variables.put("location", event.getLocation());
        variables.put("device", event.getDevice());
        variables.put("browser", event.getBrowser());
        variables.put("wasYou", event.isWasYou());
        variables.put("secureAccountLink", event.getSecureAccountLink());

        String subject = event.isCritical()
                ? "URGENT: Security Alert - " + event.getAlertType()
                : "Security Notice - " + event.getAlertType();

        EmailMessage message = EmailMessage.builder()
                .to(event.getEmail())
                .subject(subject)
                .templateName(event.getTemplateName())
                .templateVariables(variables)
                .priority(event.isCritical() ? Priority.URGENT : Priority.HIGH)
                .build();

        emailQueueService.enqueue(message);
    }
}
