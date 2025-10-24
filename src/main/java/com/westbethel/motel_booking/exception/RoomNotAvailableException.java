package com.westbethel.motel_booking.exception;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exception thrown when a room is not available for the requested dates.
 */
public class RoomNotAvailableException extends BookingException {

    private final UUID roomId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public RoomNotAvailableException(UUID roomId, LocalDate startDate, LocalDate endDate) {
        super("ROOM_NOT_AVAILABLE",
              String.format("Room %s is not available from %s to %s", roomId, startDate, endDate));
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public RoomNotAvailableException(String message) {
        super("ROOM_NOT_AVAILABLE", message);
        this.roomId = null;
        this.startDate = null;
        this.endDate = null;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
