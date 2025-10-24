package com.westbethel.motel_booking.security.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for list of sessions.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {

    private boolean success;
    private String message;
    private List<SessionResponse> sessions;
}
