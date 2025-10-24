package com.westbethel.motel_booking.notification.events;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all notification events in the system.
 * Events are published and handled by event listeners to trigger notifications.
 */
@Data
public abstract class NotificationEvent {

    /**
     * Unique identifier for this event.
     */
    private UUID eventId;

    /**
     * User ID associated with this event.
     */
    private String userId;

    /**
     * Email address to send notification to.
     */
    private String email;

    /**
     * Timestamp when the event occurred.
     */
    private OffsetDateTime occurredAt;

    /**
     * Additional metadata associated with the event.
     */
    private Map<String, Object> metadata;

    /**
     * Default constructor initializes event ID and timestamp.
     */
    protected NotificationEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = OffsetDateTime.now();
        this.metadata = new HashMap<>();
    }

    /**
     * Adds metadata to the event.
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Gets the notification template name for this event.
     * Subclasses should override to provide specific template.
     */
    public abstract String getTemplateName();
}
