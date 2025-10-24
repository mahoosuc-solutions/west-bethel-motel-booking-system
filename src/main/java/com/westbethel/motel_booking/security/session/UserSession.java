package com.westbethel.motel_booking.security.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.OffsetDateTime;

/**
 * Represents a user session stored in Redis.
 * Sessions are automatically removed from Redis after TTL expires.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("user_sessions")
public class UserSession {

    /**
     * Unique session identifier (UUID)
     */
    @Id
    private String sessionId;

    /**
     * Username of the session owner
     */
    @Indexed
    private String username;

    /**
     * IP address of the client
     */
    private String ipAddress;

    /**
     * User agent string
     */
    private String userAgent;

    /**
     * Device fingerprint for additional security
     */
    private String deviceFingerprint;

    /**
     * Timestamp when session was created
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp of last activity
     */
    private OffsetDateTime lastActivityAt;

    /**
     * Whether session is active
     */
    @Builder.Default
    private Boolean active = true;

    /**
     * JWT access token associated with this session
     */
    private String accessToken;

    /**
     * Time to live in seconds - Redis will auto-delete after this
     */
    @TimeToLive
    private Long ttl;
}
