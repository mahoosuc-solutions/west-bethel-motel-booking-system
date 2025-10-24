package com.westbethel.motel_booking.notification.preferences;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing user notification preferences.
 */
@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    /**
     * Gets the current user's notification preferences.
     */
    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences() {
        UUID userId = getCurrentUserId();
        NotificationPreferences preferences = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(NotificationPreferencesResponse.fromEntity(preferences));
    }

    /**
     * Updates the current user's notification preferences.
     */
    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @RequestBody NotificationPreferencesUpdateRequest request) {
        UUID userId = getCurrentUserId();
        NotificationPreferences updated = preferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(NotificationPreferencesResponse.fromEntity(updated));
    }

    /**
     * Resets the current user's notification preferences to defaults.
     */
    @PostMapping("/reset")
    public ResponseEntity<NotificationPreferencesResponse> resetPreferences() {
        UUID userId = getCurrentUserId();
        preferencesService.deletePreferences(userId);
        NotificationPreferences defaults = preferencesService.createDefaultPreferences(userId);
        return ResponseEntity.ok(NotificationPreferencesResponse.fromEntity(defaults));
    }

    /**
     * Gets the current authenticated user's ID from the security context.
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        // The authentication principal should contain the user ID
        // This assumes the JWT or authentication principal has the user ID as name
        String userId = authentication.getName();
        return UUID.fromString(userId);
    }
}
