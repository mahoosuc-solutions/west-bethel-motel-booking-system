package com.westbethel.motel_booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Feature flags configuration for gradual rollout and A/B testing.
 * Following FAANG best practices for feature management and progressive deployment.
 */
@Configuration
@ConfigurationProperties(prefix = "feature")
public class FeatureFlags {

    private Flags flags = new Flags();
    private MFA mfa = new MFA();
    private Email email = new Email();
    private Payment payment = new Payment();
    private Analytics analytics = new Analytics();
    private Rollout rollout = new Rollout();

    public static class Flags {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class MFA {
        private boolean enabled = true;
        private boolean required = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }

    public static class Email {
        private Notifications notifications = new Notifications();

        public static class Notifications {
            private boolean enabled = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public Notifications getNotifications() {
            return notifications;
        }

        public void setNotifications(Notifications notifications) {
            this.notifications = notifications;
        }
    }

    public static class Payment {
        private Processing processing = new Processing();

        public static class Processing {
            private boolean enabled = true;
            private boolean testMode = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isTestMode() {
                return testMode;
            }

            public void setTestMode(boolean testMode) {
                this.testMode = testMode;
            }
        }

        public Processing getProcessing() {
            return processing;
        }

        public void setProcessing(Processing processing) {
            this.processing = processing;
        }
    }

    public static class Analytics {
        private boolean enabled = true;
        private boolean detailedTracking = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDetailedTracking() {
            return detailedTracking;
        }

        public void setDetailedTracking(boolean detailedTracking) {
            this.detailedTracking = detailedTracking;
        }
    }

    public static class Rollout {
        private int percentage = 100;
        private boolean canaryEnabled = false;
        private int canaryPercentage = 10;

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = Math.min(100, Math.max(0, percentage));
        }

        public boolean isCanaryEnabled() {
            return canaryEnabled;
        }

        public void setCanaryEnabled(boolean canaryEnabled) {
            this.canaryEnabled = canaryEnabled;
        }

        public int getCanaryPercentage() {
            return canaryPercentage;
        }

        public void setCanaryPercentage(int canaryPercentage) {
            this.canaryPercentage = Math.min(100, Math.max(0, canaryPercentage));
        }
    }

    // Getters and setters
    public Flags getFlags() {
        return flags;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public MFA getMfa() {
        return mfa;
    }

    public void setMfa(MFA mfa) {
        this.mfa = mfa;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Rollout getRollout() {
        return rollout;
    }

    public void setRollout(Rollout rollout) {
        this.rollout = rollout;
    }

    // Convenience methods
    public boolean isMfaEnabled() {
        return flags.isEnabled() && mfa.isEnabled();
    }

    public boolean isEmailNotificationsEnabled() {
        return flags.isEnabled() && email.getNotifications().isEnabled();
    }

    public boolean isPaymentProcessingEnabled() {
        return flags.isEnabled() && payment.getProcessing().isEnabled();
    }

    public boolean isAnalyticsEnabled() {
        return flags.isEnabled() && analytics.isEnabled();
    }

    public boolean isUserInRollout(Long userId) {
        if (!flags.isEnabled()) {
            return false;
        }

        if (rollout.getPercentage() == 100) {
            return true;
        }

        // Use user ID to deterministically assign users to rollout percentage
        // This ensures the same user always gets the same experience
        int userBucket = (int) (userId % 100);
        return userBucket < rollout.getPercentage();
    }

    public boolean isUserInCanary(Long userId) {
        if (!flags.isEnabled() || !rollout.isCanaryEnabled()) {
            return false;
        }

        int userBucket = (int) (userId % 100);
        return userBucket < rollout.getCanaryPercentage();
    }
}
