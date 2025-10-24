package com.westbethel.motel_booking.notification.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for SMS service (Twilio integration - future feature).
 * Currently disabled, will be enabled when Twilio credentials are configured.
 */
@Configuration
public class SmsConfiguration {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    /**
     * Checks if Twilio is properly configured.
     */
    public boolean isTwilioConfigured() {
        return twilioEnabled
                && accountSid != null && !accountSid.isEmpty()
                && authToken != null && !authToken.isEmpty()
                && fromPhoneNumber != null && !fromPhoneNumber.isEmpty();
    }

    // Future: Add Twilio client bean when feature is implemented
    // @Bean
    // public Twilio twilioClient() {
    //     if (isTwilioConfigured()) {
    //         Twilio.init(accountSid, authToken);
    //         return Twilio.getRestClient();
    //     }
    //     return null;
    // }
}
