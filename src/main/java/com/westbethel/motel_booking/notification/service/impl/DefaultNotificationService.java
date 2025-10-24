package com.westbethel.motel_booking.notification.service.impl;

import com.westbethel.motel_booking.notification.model.NotificationRequest;
import com.westbethel.motel_booking.notification.model.NotificationResponse;
import com.westbethel.motel_booking.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationService.class);

    @Override
    public NotificationResponse send(NotificationRequest request) {
        log.info("Dispatching {} notification to {} with template {}", request.getChannel(), request.getRecipient(), request.getTemplateCode());
        return NotificationResponse.builder()
                .messageId("MSG-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .accepted(true)
                .build();
    }
}
