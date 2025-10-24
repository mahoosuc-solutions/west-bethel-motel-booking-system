package com.westbethel.motel_booking.security.verification;

import com.westbethel.motel_booking.security.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for email verification tokens.
 *
 * @author Security Agent 1 - Phase 2
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Find email verification token by token string.
     *
     * @param token the token string
     * @return optional email verification token
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Find valid (not expired, not verified) token.
     *
     * @param token the token string
     * @param now current timestamp
     * @return optional email verification token
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.token = :token " +
            "AND evt.verified = false AND evt.expiresAt > :now")
    Optional<EmailVerificationToken> findValidToken(@Param("token") String token,
                                                     @Param("now") OffsetDateTime now);

    /**
     * Find the most recent unverified token for a user.
     *
     * @param user the user
     * @return optional email verification token
     */
    @Query("SELECT evt FROM EmailVerificationToken evt WHERE evt.user = :user " +
            "AND evt.verified = false ORDER BY evt.createdAt DESC")
    Optional<EmailVerificationToken> findMostRecentUnverifiedToken(@Param("user") User user);

    /**
     * Delete expired tokens.
     *
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);
}
