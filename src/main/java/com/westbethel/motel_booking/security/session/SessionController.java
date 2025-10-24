package com.westbethel.motel_booking.security.session;

import com.westbethel.motel_booking.security.session.dto.SessionResponse;
import com.westbethel.motel_booking.security.session.dto.SessionListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for session management operations.
 *
 * @author Security Agent 1 - Phase 2
 */
@RestController
@RequestMapping("/api/v1/users/me/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionManagementService sessionManagementService;

    /**
     * Get all active sessions for the authenticated user.
     *
     * @param authentication the authenticated user
     * @return list of active sessions
     */
    @GetMapping
    public ResponseEntity<SessionListResponse> getActiveSessions(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<UserSession> sessions = sessionManagementService.getActiveSessions(username);

            List<SessionResponse> sessionResponses = sessions.stream()
                    .map(this::toSessionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    SessionListResponse.builder()
                            .success(true)
                            .sessions(sessionResponses)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error retrieving active sessions", e);
            return ResponseEntity.internalServerError()
                    .body(SessionListResponse.builder()
                            .success(false)
                            .message("An error occurred while retrieving sessions")
                            .build());
        }
    }

    /**
     * Invalidate a specific session.
     *
     * @param sessionId the session ID
     * @param authentication the authenticated user
     * @return success response
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> invalidateSession(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            sessionManagementService.invalidateSession(sessionId, username);

            return ResponseEntity.ok(
                    SessionResponse.builder()
                            .success(true)
                            .message("Session invalidated successfully")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(SessionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error invalidating session", e);
            return ResponseEntity.internalServerError()
                    .body(SessionResponse.builder()
                            .success(false)
                            .message("An error occurred while invalidating session")
                            .build());
        }
    }

    /**
     * Invalidate all sessions (logout from all devices).
     *
     * @param authentication the authenticated user
     * @return success response
     */
    @DeleteMapping
    public ResponseEntity<SessionResponse> invalidateAllSessions(Authentication authentication) {
        try {
            String username = authentication.getName();
            sessionManagementService.invalidateAllSessions(username);

            return ResponseEntity.ok(
                    SessionResponse.builder()
                            .success(true)
                            .message("All sessions invalidated successfully")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error invalidating all sessions", e);
            return ResponseEntity.internalServerError()
                    .body(SessionResponse.builder()
                            .success(false)
                            .message("An error occurred while invalidating sessions")
                            .build());
        }
    }

    /**
     * Convert UserSession to SessionResponse DTO.
     *
     * @param session the user session
     * @return session response DTO
     */
    private SessionResponse toSessionResponse(UserSession session) {
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .active(session.getActive())
                .build();
    }
}
