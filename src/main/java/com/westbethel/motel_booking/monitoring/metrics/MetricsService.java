package com.westbethel.motel_booking.monitoring.metrics;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Metrics Service for recording business and technical metrics
 *
 * Provides high-level methods for recording metrics throughout the application.
 * Acts as a facade to the BusinessMetrics component.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final BusinessMetrics businessMetrics;

    /**
     * Record a booking creation event
     */
    public void recordBookingCreated(BigDecimal amount) {
        businessMetrics.incrementBookingsCreated();
        if (amount != null) {
            businessMetrics.addRevenue(amount.multiply(BigDecimal.valueOf(100)).longValue());
        }
        log.debug("Recorded booking creation with amount: {}", amount);
    }

    /**
     * Record a booking cancellation event
     */
    public void recordBookingCancelled() {
        businessMetrics.incrementBookingsCancelled();
        log.debug("Recorded booking cancellation");
    }

    /**
     * Record a successful payment
     */
    public void recordPaymentSuccess(BigDecimal amount) {
        businessMetrics.incrementPaymentsSuccess();
        if (amount != null) {
            businessMetrics.addRevenue(amount.multiply(BigDecimal.valueOf(100)).longValue());
        }
        log.debug("Recorded successful payment with amount: {}", amount);
    }

    /**
     * Record a failed payment
     */
    public void recordPaymentFailure(String reason) {
        businessMetrics.incrementPaymentsFailure();
        log.debug("Recorded payment failure: {}", reason);
    }

    /**
     * Record a user registration
     */
    public void recordUserRegistration() {
        businessMetrics.incrementUserRegistrations();
        log.debug("Recorded user registration");
    }

    /**
     * Record a successful authentication attempt
     */
    public void recordAuthenticationSuccess(String username) {
        businessMetrics.incrementAuthSuccess();
        log.debug("Recorded successful authentication for user: {}", username);
    }

    /**
     * Record a failed authentication attempt
     */
    public void recordAuthenticationFailure(String username, String reason) {
        businessMetrics.incrementAuthFailure();
        log.warn("Recorded failed authentication for user: {} - Reason: {}", username, reason);
    }

    /**
     * Record a cache hit
     */
    public void recordCacheHit(String cacheName) {
        businessMetrics.incrementCacheHits();
        log.trace("Cache hit for cache: {}", cacheName);
    }

    /**
     * Record a cache miss
     */
    public void recordCacheMiss(String cacheName) {
        businessMetrics.incrementCacheMisses();
        log.trace("Cache miss for cache: {}", cacheName);
    }

    /**
     * Record an email sent successfully
     */
    public void recordEmailSent(String recipient) {
        businessMetrics.incrementEmailsSent();
        log.debug("Recorded email sent to: {}", recipient);
    }

    /**
     * Record an email send failure
     */
    public void recordEmailFailed(String recipient, String reason) {
        businessMetrics.incrementEmailsFailed();
        log.warn("Recorded email failure for: {} - Reason: {}", recipient, reason);
    }

    /**
     * Update active user sessions count
     */
    public void updateActiveUserSessions(int count) {
        businessMetrics.setActiveUserSessions(count);
        log.trace("Updated active user sessions: {}", count);
    }

    /**
     * Increment active user sessions
     */
    public void incrementActiveUserSessions() {
        businessMetrics.incrementActiveUserSessions();
        log.trace("Incremented active user sessions");
    }

    /**
     * Decrement active user sessions
     */
    public void decrementActiveUserSessions() {
        businessMetrics.decrementActiveUserSessions();
        log.trace("Decremented active user sessions");
    }

    /**
     * Update email queue size
     */
    public void updateEmailQueueSize(int size) {
        businessMetrics.setEmailQueueSize(size);
        log.trace("Updated email queue size: {}", size);
    }

    /**
     * Update database connection pool size
     */
    public void updateDatabaseConnectionPoolSize(int size) {
        businessMetrics.setDatabaseConnectionPoolSize(size);
        log.trace("Updated database connection pool size: {}", size);
    }

    /**
     * Record JWT validation time
     */
    public <T> T recordJwtValidation(Supplier<T> operation) {
        return Timer.Sample.builder()
            .register(businessMetrics.getJwtValidationTimer().getRegistry())
            .stop(businessMetrics.getJwtValidationTimer());
    }

    /**
     * Record JWT validation time with explicit duration
     */
    public void recordJwtValidationTime(long durationMs) {
        businessMetrics.getJwtValidationTimer().record(durationMs, TimeUnit.MILLISECONDS);
        log.trace("Recorded JWT validation time: {}ms", durationMs);
    }

    /**
     * Record database query execution time
     */
    public void recordDatabaseQueryTime(long durationMs) {
        businessMetrics.getDatabaseQueryTimer().record(durationMs, TimeUnit.MILLISECONDS);
        log.trace("Recorded database query time: {}ms", durationMs);
    }

    /**
     * Record cache operation time
     */
    public void recordCacheOperationTime(long durationMs) {
        businessMetrics.getCacheOperationTimer().record(durationMs, TimeUnit.MILLISECONDS);
        log.trace("Recorded cache operation time: {}ms", durationMs);
    }

    /**
     * Record email send time
     */
    public void recordEmailSendTime(long durationMs) {
        businessMetrics.getEmailSendTimer().record(durationMs, TimeUnit.MILLISECONDS);
        log.trace("Recorded email send time: {}ms", durationMs);
    }

    /**
     * Record payment processing time
     */
    public void recordPaymentProcessingTime(long durationMs) {
        businessMetrics.getPaymentProcessingTimer().record(durationMs, TimeUnit.MILLISECONDS);
        log.trace("Recorded payment processing time: {}ms", durationMs);
    }

    /**
     * Get cache hit ratio
     */
    public double getCacheHitRatio() {
        return businessMetrics.getCacheHitRatio();
    }

    /**
     * Get payment success rate
     */
    public double getPaymentSuccessRate() {
        return businessMetrics.getPaymentSuccessRate();
    }

    /**
     * Get authentication success rate
     */
    public double getAuthSuccessRate() {
        return businessMetrics.getAuthSuccessRate();
    }

    /**
     * Execute an operation with timing
     */
    public <T> T timeOperation(Timer timer, Supplier<T> operation) {
        Timer.Sample sample = Timer.start();
        try {
            return operation.get();
        } finally {
            sample.stop(timer);
        }
    }

    /**
     * Execute a void operation with timing
     */
    public void timeOperation(Timer timer, Runnable operation) {
        Timer.Sample sample = Timer.start();
        try {
            operation.run();
        } finally {
            sample.stop(timer);
        }
    }
}
