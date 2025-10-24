package com.westbethel.motel_booking.security.session;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing user sessions in Redis.
 *
 * @author Security Agent 1 - Phase 2
 */
@Repository
public interface UserSessionRepository extends CrudRepository<UserSession, String> {

    /**
     * Find all sessions for a specific username.
     *
     * @param username the username
     * @return list of user sessions
     */
    List<UserSession> findByUsername(String username);

    /**
     * Find all active sessions for a username.
     *
     * @param username the username
     * @param active whether session is active
     * @return list of active sessions
     */
    List<UserSession> findByUsernameAndActive(String username, Boolean active);
}
