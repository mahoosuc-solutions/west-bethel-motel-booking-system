package com.westbethel.motel_booking.notification.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private final String messageId;
    private final boolean accepted;
}
