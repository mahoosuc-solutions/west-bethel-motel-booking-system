package com.westbethel.motel_booking.notification.preferences;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing notification preferences.
 */
@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {

    /**
     * Finds notification preferences by user ID.
     */
    Optional<NotificationPreferences> findByUserId(UUID userId);

    /**
     * Checks if preferences exist for a user.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Deletes preferences by user ID.
     */
    void deleteByUserId(UUID userId);
}
