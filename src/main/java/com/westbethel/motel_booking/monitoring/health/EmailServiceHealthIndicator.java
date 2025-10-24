package com.westbethel.motel_booking.monitoring.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;

/**
 * Email Service Health Indicator
 *
 * Checks email server connectivity and SMTP configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailServiceHealthIndicator implements HealthIndicator {

    private final JavaMailSender mailSender;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Test SMTP connection
            var session = mailSender.createMimeMessage().getSession();
            var transport = session.getTransport("smtp");

            try {
                transport.connect();
                long connectionTime = System.currentTimeMillis() - startTime;

                return Health.up()
                    .withDetail("service", "Email/SMTP")
                    .withDetail("connectionTime", connectionTime + "ms")
                    .withDetail("status", "Connected")
                    .build();

            } finally {
                if (transport.isConnected()) {
                    transport.close();
                }
            }

        } catch (MessagingException e) {
            log.warn("Email service health check failed", e);
            return Health.down()
                .withDetail("service", "Email/SMTP")
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        } catch (Exception e) {
            log.error("Unexpected error during email health check", e);
            return Health.down()
                .withDetail("service", "Email/SMTP")
                .withDetail("error", "Unexpected error: " + e.getMessage())
                .withException(e)
                .build();
        }
    }
}
