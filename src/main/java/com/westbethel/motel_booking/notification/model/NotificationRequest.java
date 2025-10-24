package com.westbethel.motel_booking.notification.model;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationRequest {

    public enum Channel {
        EMAIL,
        SMS
    }

    private final Channel channel;
    private final String templateCode;
    private final String recipient;
    private final Map<String, Object> variables;
    private final String locale;
}
