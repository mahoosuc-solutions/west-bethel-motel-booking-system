package com.westbethel.motel_booking.security.mfa;

import com.westbethel.motel_booking.security.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for MFA backup codes.
 *
 * @author Security Agent 1 - Phase 2
 */
@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, UUID> {

    /**
     * Find all backup codes for a user.
     *
     * @param user the user
     * @return list of backup codes
     */
    List<MfaBackupCode> findByUser(User user);

    /**
     * Find unused backup codes for a user.
     *
     * @param user the user
     * @return list of unused backup codes
     */
    @Query("SELECT mbc FROM MfaBackupCode mbc WHERE mbc.user = :user AND mbc.used = false")
    List<MfaBackupCode> findUnusedByUser(@Param("user") User user);

    /**
     * Delete all backup codes for a user.
     *
     * @param user the user
     * @return number of deleted codes
     */
    @Modifying
    @Query("DELETE FROM MfaBackupCode mbc WHERE mbc.user = :user")
    int deleteByUser(@Param("user") User user);
}
