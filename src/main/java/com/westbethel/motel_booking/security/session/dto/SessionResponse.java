package com.westbethel.motel_booking.security.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for session information.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private boolean success;
    private String message;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastActivityAt;
    private Boolean active;
}
