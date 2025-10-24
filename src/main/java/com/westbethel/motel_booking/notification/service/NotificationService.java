package com.westbethel.motel_booking.notification.service;

import com.westbethel.motel_booking.notification.model.NotificationRequest;
import com.westbethel.motel_booking.notification.model.NotificationResponse;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request);
}
