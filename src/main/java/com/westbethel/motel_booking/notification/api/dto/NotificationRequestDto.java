package com.westbethel.motel_booking.notification.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequestDto {

    public enum Channel {
        EMAIL,
        SMS
    }

    @NotNull(message = "Notification channel is required")
    private Channel channel;

    @NotBlank(message = "Template code is required")
    private String templateCode;

    @NotBlank(message = "Recipient is required")
    @Email(message = "Recipient must be a valid email address", groups = EmailValidation.class)
    private String recipient;

    private Map<String, Object> variables;

    private String locale;

    public interface EmailValidation {}
}
