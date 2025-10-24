package com.westbethel.motel_booking.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.util.Map;

/**
 * Service for sending emails with support for templates, HTML content, and attachments.
 * Email sending is performed asynchronously to avoid blocking the main application thread.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine templateEngine;

    @Value("${notification.from-address:noreply@westbethelmotel.com}")
    private String defaultFromAddress;

    @Value("${notification.from-name:West Bethel Motel}")
    private String defaultFromName;

    /**
     * Sends an email message asynchronously.
     *
     * @param message the email message to send
     * @throws EmailSendingException if email sending fails
     */
    @Async
    public void sendEmail(EmailMessage message) {
        try {
            validateEmailMessage(message);

            if (message.isTemplateEmail()) {
                sendTemplatedEmail(message);
            } else if (message.getHtmlBody() != null && !message.getHtmlBody().isEmpty()) {
                sendHtmlEmail(message);
            } else {
                sendSimpleEmail(message);
            }

            log.info("Email sent successfully to: {}", message.getTo());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", message.getTo(), e);
            throw new EmailSendingException("Failed to send email to: " + message.getTo(), e);
        }
    }

    /**
     * Sends a template-based email.
     *
     * @param to the recipient email address
     * @param templateName the name of the Thymeleaf template
     * @param variables the variables to use in the template
     */
    public void sendTemplateEmail(String to, String templateName, Map<String, Object> variables) {
        EmailMessage message = EmailMessage.builder()
                .to(to)
                .templateName(templateName)
                .templateVariables(variables)
                .build();

        sendEmail(message);
    }

    /**
     * Sends an HTML email.
     *
     * @param to the recipient email address
     * @param subject the email subject
     * @param htmlBody the HTML content
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        EmailMessage message = EmailMessage.builder()
                .to(to)
                .subject(subject)
                .htmlBody(htmlBody)
                .build();

        sendEmail(message);
    }

    /**
     * Sends an email with an attachment.
     *
     * @param message the email message
     * @param attachment the file to attach
     */
    public void sendEmailWithAttachment(EmailMessage message, File attachment) {
        message.addAttachment(attachment);
        sendEmail(message);
    }

    /**
     * Sends a simple plain text email.
     */
    private void sendSimpleEmail(EmailMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(getFromAddress(message));
        mailMessage.setTo(message.getTo());

        if (message.getCc() != null && !message.getCc().isEmpty()) {
            mailMessage.setCc(message.getCc().toArray(new String[0]));
        }

        if (message.getBcc() != null && !message.getBcc().isEmpty()) {
            mailMessage.setBcc(message.getBcc().toArray(new String[0]));
        }

        if (message.getReplyTo() != null) {
            mailMessage.setReplyTo(message.getReplyTo());
        }

        mailMessage.setSubject(message.getSubject());
        mailMessage.setText(message.getBody());

        mailSender.send(mailMessage);
    }

    /**
     * Sends an HTML email with optional attachments.
     */
    private void sendHtmlEmail(EmailMessage message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                message.hasAttachments(),
                "UTF-8"
        );

        helper.setFrom(getFromAddress(message), defaultFromName);
        helper.setTo(message.getTo());

        if (message.getCc() != null && !message.getCc().isEmpty()) {
            helper.setCc(message.getCc().toArray(new String[0]));
        }

        if (message.getBcc() != null && !message.getBcc().isEmpty()) {
            helper.setBcc(message.getBcc().toArray(new String[0]));
        }

        if (message.getReplyTo() != null) {
            helper.setReplyTo(message.getReplyTo());
        }

        helper.setSubject(message.getSubject());

        // Set HTML content with plain text alternative
        String htmlContent = message.getHtmlBody();
        String plainTextContent = message.getBody() != null ? message.getBody() : extractTextFromHtml(htmlContent);
        helper.setText(plainTextContent, htmlContent);

        // Add attachments if present
        if (message.hasAttachments()) {
            for (File attachment : message.getAttachments()) {
                helper.addAttachment(attachment.getName(), attachment);
            }
        }

        mailSender.send(mimeMessage);
    }

    /**
     * Sends a template-based email.
     */
    private void sendTemplatedEmail(EmailMessage message) throws MessagingException {
        // Render the template
        Context context = new Context();
        if (message.getTemplateVariables() != null) {
            context.setVariables(message.getTemplateVariables());
        }

        String htmlContent = templateEngine.process(message.getTemplateName(), context);

        // Set the HTML body and send
        message.setHtmlBody(htmlContent);
        sendHtmlEmail(message);
    }

    /**
     * Validates the email message before sending.
     */
    private void validateEmailMessage(EmailMessage message) {
        if (message.getTo() == null || message.getTo().isEmpty()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }

        if (message.getSubject() == null || message.getSubject().isEmpty()) {
            // Auto-generate subject for template emails if not provided
            if (!message.isTemplateEmail()) {
                throw new IllegalArgumentException("Email subject is required");
            }
        }

        if (!message.isTemplateEmail() &&
                (message.getBody() == null || message.getBody().isEmpty()) &&
                (message.getHtmlBody() == null || message.getHtmlBody().isEmpty())) {
            throw new IllegalArgumentException("Email body or HTML body is required");
        }

        if (message.isTemplateEmail() && (message.getTemplateName() == null || message.getTemplateName().isEmpty())) {
            throw new IllegalArgumentException("Template name is required for template emails");
        }
    }

    /**
     * Gets the from address, using default if not specified in message.
     */
    private String getFromAddress(EmailMessage message) {
        return message.getFrom() != null && !message.getFrom().isEmpty()
                ? message.getFrom()
                : defaultFromAddress;
    }

    /**
     * Extracts plain text from HTML content (basic implementation).
     */
    private String extractTextFromHtml(String html) {
        if (html == null) {
            return "";
        }
        // Remove HTML tags for plain text alternative
        return html.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * Custom exception for email sending failures.
     */
    public static class EmailSendingException extends RuntimeException {
        public EmailSendingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
