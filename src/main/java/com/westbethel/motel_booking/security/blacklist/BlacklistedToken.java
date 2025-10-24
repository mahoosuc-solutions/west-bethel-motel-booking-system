package com.westbethel.motel_booking.security.blacklist;

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
 * Represents a blacklisted JWT token stored in Redis.
 * Tokens are automatically removed from Redis after TTL expires.
 *
 * @author Security Agent 1 - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("blacklisted_tokens")
public class BlacklistedToken {

    /**
     * The JWT token (used as primary key)
     */
    @Id
    private String token;

    /**
     * Username of the token owner
     */
    @Indexed
    private String username;

    /**
     * Timestamp when token was blacklisted
     */
    private OffsetDateTime blacklistedAt;

    /**
     * Timestamp when token originally expires
     */
    private OffsetDateTime expiresAt;

    /**
     * Reason for blacklisting (e.g., "LOGOUT", "ADMIN_REVOCATION", "PASSWORD_CHANGE")
     */
    private String reason;

    /**
     * Time to live in seconds - Redis will auto-delete after this
     * Should be set to remaining token validity period
     */
    @TimeToLive
    private Long ttl;
}
