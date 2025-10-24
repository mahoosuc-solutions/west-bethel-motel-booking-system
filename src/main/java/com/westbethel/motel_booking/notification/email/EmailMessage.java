package com.westbethel.motel_booking.notification.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an email message with all necessary information for sending.
 * Supports both simple text emails and template-based HTML emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary recipient email address (required).
     */
    private String to;

    /**
     * CC recipients (optional).
     */
    @Builder.Default
    private List<String> cc = new ArrayList<>();

    /**
     * BCC recipients (optional).
     */
    @Builder.Default
    private List<String> bcc = new ArrayList<>();

    /**
     * Sender email address (optional, uses default if not provided).
     */
    private String from;

    /**
     * Reply-to email address (optional).
     */
    private String replyTo;

    /**
     * Email subject (required).
     */
    private String subject;

    /**
     * Plain text email body (optional if htmlBody is provided).
     */
    private String body;

    /**
     * HTML email body (optional if body is provided).
     */
    private String htmlBody;

    /**
     * Variables to be used when rendering email template.
     */
    @Builder.Default
    private Map<String, Object> templateVariables = new HashMap<>();

    /**
     * Name of the Thymeleaf template to use (optional).
     * If provided, template will be rendered with templateVariables.
     */
    private String templateName;

    /**
     * File attachments (optional).
     * Note: Attachments are not serialized for Redis queue.
     */
    @Builder.Default
    private transient List<File> attachments = new ArrayList<>();

    /**
     * Email priority for queue processing.
     */
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * Whether a delivery receipt is required (optional).
     */
    @Builder.Default
    private boolean requiresDeliveryReceipt = false;

    /**
     * Adds a CC recipient.
     */
    public void addCc(String email) {
        if (this.cc == null) {
            this.cc = new ArrayList<>();
        }
        this.cc.add(email);
    }

    /**
     * Adds a BCC recipient.
     */
    public void addBcc(String email) {
        if (this.bcc == null) {
            this.bcc = new ArrayList<>();
        }
        this.bcc.add(email);
    }

    /**
     * Adds an attachment.
     */
    public void addAttachment(File file) {
        if (this.attachments == null) {
            this.attachments = new ArrayList<>();
        }
        this.attachments.add(file);
    }

    /**
     * Adds a template variable.
     */
    public void addTemplateVariable(String key, Object value) {
        if (this.templateVariables == null) {
            this.templateVariables = new HashMap<>();
        }
        this.templateVariables.put(key, value);
    }

    /**
     * Checks if this is a template-based email.
     */
    public boolean isTemplateEmail() {
        return templateName != null && !templateName.isEmpty();
    }

    /**
     * Checks if this email has attachments.
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}
