package com.westbethel.motel_booking.service;

import com.westbethel.motel_booking.config.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing feature flags and gradual rollout.
 * Provides centralized access to feature flag state and rollout logic.
 */
@Service
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlags featureFlags;

    public FeatureFlagService(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    /**
     * Check if MFA is enabled for the system.
     */
    public boolean isMfaEnabled() {
        boolean enabled = featureFlags.isMfaEnabled();
        logger.debug("MFA enabled check: {}", enabled);
        return enabled;
    }

    /**
     * Check if MFA is required for all users.
     */
    public boolean isMfaRequired() {
        boolean required = featureFlags.isMfaEnabled() && featureFlags.getMfa().isRequired();
        logger.debug("MFA required check: {}", required);
        return required;
    }

    /**
     * Check if email notifications are enabled.
     */
    public boolean isEmailNotificationsEnabled() {
        boolean enabled = featureFlags.isEmailNotificationsEnabled();
        logger.debug("Email notifications enabled check: {}", enabled);
        return enabled;
    }

    /**
     * Check if payment processing is enabled.
     */
    public boolean isPaymentProcessingEnabled() {
        boolean enabled = featureFlags.isPaymentProcessingEnabled();
        logger.debug("Payment processing enabled check: {}", enabled);
        return enabled;
    }

    /**
     * Check if payment processing is in test mode.
     */
    public boolean isPaymentTestMode() {
        boolean testMode = featureFlags.getPayment().getProcessing().isTestMode();
        logger.debug("Payment test mode check: {}", testMode);
        return testMode;
    }

    /**
     * Check if analytics tracking is enabled.
     */
    public boolean isAnalyticsEnabled() {
        boolean enabled = featureFlags.isAnalyticsEnabled();
        logger.debug("Analytics enabled check: {}", enabled);
        return enabled;
    }

    /**
     * Check if detailed analytics tracking is enabled.
     */
    public boolean isDetailedAnalyticsEnabled() {
        boolean enabled = featureFlags.isAnalyticsEnabled()
            && featureFlags.getAnalytics().isDetailedTracking();
        logger.debug("Detailed analytics enabled check: {}", enabled);
        return enabled;
    }

    /**
     * Check if a user is in the rollout percentage.
     * Uses deterministic assignment based on user ID.
     */
    public boolean isUserInRollout(Long userId) {
        boolean inRollout = featureFlags.isUserInRollout(userId);
        logger.debug("User {} rollout check: {}", userId, inRollout);
        return inRollout;
    }

    /**
     * Check if a user is in the canary deployment.
     */
    public boolean isUserInCanary(Long userId) {
        boolean inCanary = featureFlags.isUserInCanary(userId);
        logger.debug("User {} canary check: {}", userId, inCanary);
        return inCanary;
    }

    /**
     * Get current rollout percentage.
     */
    public int getRolloutPercentage() {
        return featureFlags.getRollout().getPercentage();
    }

    /**
     * Get current canary percentage.
     */
    public int getCanaryPercentage() {
        return featureFlags.getRollout().getCanaryPercentage();
    }

    /**
     * Check if canary deployment is enabled.
     */
    public boolean isCanaryEnabled() {
        return featureFlags.getRollout().isCanaryEnabled();
    }

    /**
     * Get all feature flags status for monitoring/admin dashboard.
     */
    public FeatureFlagsStatus getFeatureFlagsStatus() {
        return new FeatureFlagsStatus(
            featureFlags.isMfaEnabled(),
            featureFlags.getMfa().isRequired(),
            featureFlags.isEmailNotificationsEnabled(),
            featureFlags.isPaymentProcessingEnabled(),
            featureFlags.getPayment().getProcessing().isTestMode(),
            featureFlags.isAnalyticsEnabled(),
            featureFlags.getAnalytics().isDetailedTracking(),
            featureFlags.getRollout().getPercentage(),
            featureFlags.getRollout().isCanaryEnabled(),
            featureFlags.getRollout().getCanaryPercentage()
        );
    }

    /**
     * DTO for feature flags status.
     */
    public record FeatureFlagsStatus(
        boolean mfaEnabled,
        boolean mfaRequired,
        boolean emailNotificationsEnabled,
        boolean paymentProcessingEnabled,
        boolean paymentTestMode,
        boolean analyticsEnabled,
        boolean detailedAnalytics,
        int rolloutPercentage,
        boolean canaryEnabled,
        int canaryPercentage
    ) {}
}
