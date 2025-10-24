package com.westbethel.motel_booking.notification.email;

/**
 * Email priority levels for queue processing.
 * Higher priority emails are processed first.
 */
public enum Priority {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
