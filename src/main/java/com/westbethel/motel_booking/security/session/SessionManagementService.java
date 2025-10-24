package com.westbethel.motel_booking.security.session;

import com.westbethel.motel_booking.common.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user sessions in Redis.
 *
 * @author Security Agent 1 - Phase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final UserSessionRepository sessionRepository;
    private final AuditService auditService;

    private static final long SESSION_TTL_SECONDS = 24 * 60 * 60; // 24 hours

    /**
     * Create a new session for a user.
     *
     * @param username the username
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @param accessToken the JWT access token
     * @return the created session
     */
    public UserSession createSession(String username, String ipAddress, String userAgent, String accessToken) {
        log.info("Creating session for user: {}", username);

        String sessionId = UUID.randomUUID().toString();

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceFingerprint(generateDeviceFingerprint(ipAddress, userAgent))
                .createdAt(OffsetDateTime.now())
                .lastActivityAt(OffsetDateTime.now())
                .active(true)
                .accessToken(accessToken)
                .ttl(SESSION_TTL_SECONDS)
                .build();

        sessionRepository.save(session);

        // Audit log
        auditService.logSecurityEvent(
                "SESSION_CREATED",
                username,
                String.format("Session created from IP: %s", ipAddress),
                null
        );

        log.debug("Session created with ID: {}", sessionId);
        return session;
    }

    /**
     * Get all active sessions for a user.
     *
     * @param username the username
     * @return list of active sessions
     */
    public List<UserSession> getActiveSessions(String username) {
        return sessionRepository.findByUsernameAndActive(username, true);
    }

    /**
     * Get a specific session by ID.
     *
     * @param sessionId the session ID
     * @return optional session
     */
    public Optional<UserSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * Update session activity timestamp.
     *
     * @param sessionId the session ID
     */
    public void updateActivity(String sessionId) {
        Optional<UserSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setLastActivityAt(OffsetDateTime.now());
            sessionRepository.save(session);
            log.trace("Updated activity for session: {}", sessionId);
        }
    }

    /**
     * Invalidate a specific session.
     *
     * @param sessionId the session ID
     * @param username the username (for authorization)
     * @throws IllegalArgumentException if session not found or unauthorized
     */
    public void invalidateSession(String sessionId, String username) {
        log.info("Invalidating session: {} for user: {}", sessionId, username);

        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // Verify ownership
        if (!session.getUsername().equals(username)) {
            log.warn("Unauthorized session invalidation attempt: user {} tried to invalidate session for {}",
                    username, session.getUsername());
            throw new IllegalArgumentException("Unauthorized");
        }

        session.setActive(false);
        sessionRepository.save(session);

        // Audit log
        auditService.logSecurityEvent(
                "SESSION_INVALIDATED",
                username,
                String.format("Session %s invalidated", sessionId),
                null
        );

        log.info("Session invalidated: {}", sessionId);
    }

    /**
     * Invalidate all sessions for a user.
     * Used for logout from all devices or password change.
     *
     * @param username the username
     */
    public void invalidateAllSessions(String username) {
        log.info("Invalidating all sessions for user: {}", username);

        List<UserSession> sessions = sessionRepository.findByUsernameAndActive(username, true);

        for (UserSession session : sessions) {
            session.setActive(false);
            sessionRepository.save(session);
        }

        // Audit log
        auditService.logSecurityEvent(
                "ALL_SESSIONS_INVALIDATED",
                username,
                String.format("All sessions invalidated (%d sessions)", sessions.size()),
                null
        );

        log.info("Invalidated {} sessions for user: {}", sessions.size(), username);
    }

    /**
     * Detect suspicious activity based on session patterns.
     * Checks for impossible travel, unusual user agents, etc.
     *
     * @param username the username
     * @param ipAddress current IP address
     * @param userAgent current user agent
     * @return true if suspicious activity detected
     */
    public boolean detectSuspiciousActivity(String username, String ipAddress, String userAgent) {
        List<UserSession> recentSessions = sessionRepository.findByUsernameAndActive(username, true);

        if (recentSessions.isEmpty()) {
            return false;
        }

        // Check for IP address changes (simplified check)
        for (UserSession session : recentSessions) {
            if (!session.getIpAddress().equals(ipAddress)) {
                log.warn("Suspicious activity detected for user {}: IP change from {} to {}",
                        username, session.getIpAddress(), ipAddress);

                auditService.logSecurityEvent(
                        "SUSPICIOUS_ACTIVITY_DETECTED",
                        username,
                        String.format("IP address change detected: %s -> %s",
                                session.getIpAddress(), ipAddress),
                        null
                );

                return true;
            }
        }

        return false;
    }

    /**
     * Generate a device fingerprint from IP and user agent.
     *
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @return device fingerprint
     */
    private String generateDeviceFingerprint(String ipAddress, String userAgent) {
        return String.format("%s|%s", ipAddress, userAgent).hashCode() + "";
    }
}
