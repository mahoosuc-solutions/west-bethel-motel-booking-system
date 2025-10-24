package com.westbethel.motel_booking.notification.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending test emails from admin panel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEmailRequest {

    private String to;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, Object> templateVariables;
}
