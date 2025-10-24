package com.westbethel.motel_booking.security.blacklist;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing blacklisted tokens in Redis.
 *
 * @author Security Agent 1 - Phase 2
 */
@Repository
public interface TokenBlacklistRepository extends CrudRepository<BlacklistedToken, String> {

    /**
     * Find all blacklisted tokens for a specific username.
     * Useful for revoking all tokens when user changes password or logs out from all devices.
     *
     * @param username the username
     * @return list of blacklisted tokens for the user
     */
    List<BlacklistedToken> findByUsername(String username);

    /**
     * Check if a token exists in the blacklist.
     *
     * @param token the JWT token
     * @return true if token is blacklisted
     */
    boolean existsByToken(String token);
}
