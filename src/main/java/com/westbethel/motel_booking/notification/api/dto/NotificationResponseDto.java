package com.westbethel.motel_booking.notification.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponseDto {

    private final String messageId;
    private final boolean accepted;
    private final String status;
}
