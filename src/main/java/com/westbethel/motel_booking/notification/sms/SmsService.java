package com.westbethel.motel_booking.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS service stub for future Twilio integration.
 * Currently logs SMS messages instead of actually sending them.
 */
@Service
@Slf4j
public class SmsService {

    /**
     * Sends an SMS message (stub implementation).
     * In production, this would integrate with Twilio or another SMS provider.
     *
     * @param phoneNumber the recipient phone number
     * @param message the message content
     */
    public void sendSms(String phoneNumber, String message) {
        log.info("SMS sending not yet implemented. Would send to {}: {}", phoneNumber, message);
        // Future implementation:
        // - Validate phone number format
        // - Use Twilio client to send SMS
        // - Handle delivery status
        // - Queue for retry on failure
    }

    /**
     * Sends an SMS with a template (stub implementation).
     *
     * @param phoneNumber the recipient phone number
     * @param templateName the SMS template name
     * @param variables template variables
     */
    public void sendTemplateSms(String phoneNumber, String templateName, Object... variables) {
        log.info("SMS template sending not yet implemented. Template: {}, Recipient: {}",
                templateName, phoneNumber);
        // Future implementation:
        // - Load template from configuration
        // - Replace variables in template
        // - Send via sendSms()
    }

    /**
     * Validates a phone number format (stub implementation).
     *
     * @param phoneNumber the phone number to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Basic validation - in production, use libphonenumber library
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        return cleaned.length() >= 10 && cleaned.length() <= 15;
    }
}
