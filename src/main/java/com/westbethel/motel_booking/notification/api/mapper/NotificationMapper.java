package com.westbethel.motel_booking.notification.api.mapper;

import com.westbethel.motel_booking.notification.api.dto.NotificationRequestDto;
import com.westbethel.motel_booking.notification.api.dto.NotificationResponseDto;
import com.westbethel.motel_booking.notification.model.NotificationRequest;
import com.westbethel.motel_booking.notification.model.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationRequest toModel(NotificationRequestDto dto) {
        return NotificationRequest.builder()
                .channel(NotificationRequest.Channel.valueOf(dto.getChannel().name()))
                .templateCode(dto.getTemplateCode())
                .recipient(dto.getRecipient())
                .variables(dto.getVariables())
                .locale(dto.getLocale())
                .build();
    }

    public NotificationResponseDto toDto(NotificationResponse response) {
        return NotificationResponseDto.builder()
                .messageId(response.getMessageId())
                .accepted(response.isAccepted())
                .status(response.isAccepted() ? "SENT" : "FAILED")
                .build();
    }
}
