package com.westbethel.motel_booking.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Business Metrics for West Bethel Motel
 *
 * Tracks key business metrics including bookings, payments, authentication,
 * and other critical business events.
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter bookingsCreatedCounter;
    private final Counter bookingsCancelledCounter;
    private final Counter paymentsSuccessCounter;
    private final Counter paymentsFailureCounter;
    private final Counter userRegistrationCounter;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter emailsSentCounter;
    private final Counter emailsFailedCounter;

    // Gauges (using atomic references)
    private final AtomicInteger activeUserSessions = new AtomicInteger(0);
    private final AtomicInteger emailQueueSize = new AtomicInteger(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicInteger databaseConnectionPoolSize = new AtomicInteger(0);

    // Timers
    private final Timer jwtValidationTimer;
    private final Timer databaseQueryTimer;
    private final Timer cacheOperationTimer;
    private final Timer emailSendTimer;
    private final Timer paymentProcessingTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize Counters
        this.bookingsCreatedCounter = Counter.builder("bookings.created")
            .description("Total number of bookings created")
            .tag("type", "business")
            .register(meterRegistry);

        this.bookingsCancelledCounter = Counter.builder("bookings.cancelled")
            .description("Total number of bookings cancelled")
            .tag("type", "business")
            .register(meterRegistry);

        this.paymentsSuccessCounter = Counter.builder("payments.success")
            .description("Total number of successful payments")
            .tag("type", "business")
            .register(meterRegistry);

        this.paymentsFailureCounter = Counter.builder("payments.failure")
            .description("Total number of failed payments")
            .tag("type", "business")
            .register(meterRegistry);

        this.userRegistrationCounter = Counter.builder("users.registered")
            .description("Total number of user registrations")
            .tag("type", "business")
            .register(meterRegistry);

        this.authSuccessCounter = Counter.builder("auth.success")
            .description("Total number of successful authentications")
            .tag("type", "security")
            .register(meterRegistry);

        this.authFailureCounter = Counter.builder("auth.failure")
            .description("Total number of failed authentications")
            .tag("type", "security")
            .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("cache.hits")
            .description("Total number of cache hits")
            .tag("type", "performance")
            .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("cache.misses")
            .description("Total number of cache misses")
            .tag("type", "performance")
            .register(meterRegistry);

        this.emailsSentCounter = Counter.builder("emails.sent")
            .description("Total number of emails sent successfully")
            .tag("type", "notification")
            .register(meterRegistry);

        this.emailsFailedCounter = Counter.builder("emails.failed")
            .description("Total number of failed email sends")
            .tag("type", "notification")
            .register(meterRegistry);

        // Initialize Gauges
        Gauge.builder("sessions.active", activeUserSessions, AtomicInteger::get)
            .description("Number of active user sessions")
            .tag("type", "business")
            .register(meterRegistry);

        Gauge.builder("email.queue.size", emailQueueSize, AtomicInteger::get)
            .description("Current email queue size")
            .tag("type", "notification")
            .register(meterRegistry);

        Gauge.builder("revenue.total", totalRevenue, AtomicLong::get)
            .description("Total revenue in cents")
            .tag("type", "business")
            .register(meterRegistry);

        Gauge.builder("database.connections.active", databaseConnectionPoolSize, AtomicInteger::get)
            .description("Active database connections")
            .tag("type", "infrastructure")
            .register(meterRegistry);

        // Initialize Timers
        this.jwtValidationTimer = Timer.builder("jwt.validation.time")
            .description("Time taken to validate JWT tokens")
            .tag("type", "security")
            .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("database.query.time")
            .description("Time taken to execute database queries")
            .tag("type", "infrastructure")
            .register(meterRegistry);

        this.cacheOperationTimer = Timer.builder("cache.operation.time")
            .description("Time taken for cache operations")
            .tag("type", "performance")
            .register(meterRegistry);

        this.emailSendTimer = Timer.builder("email.send.time")
            .description("Time taken to send emails")
            .tag("type", "notification")
            .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("payment.processing.time")
            .description("Time taken to process payments")
            .tag("type", "business")
            .register(meterRegistry);

        log.info("Business metrics initialized successfully");
    }

    // Counter Methods
    public void incrementBookingsCreated() {
        bookingsCreatedCounter.increment();
    }

    public void incrementBookingsCancelled() {
        bookingsCancelledCounter.increment();
    }

    public void incrementPaymentsSuccess() {
        paymentsSuccessCounter.increment();
    }

    public void incrementPaymentsFailure() {
        paymentsFailureCounter.increment();
    }

    public void incrementUserRegistrations() {
        userRegistrationCounter.increment();
    }

    public void incrementAuthSuccess() {
        authSuccessCounter.increment();
    }

    public void incrementAuthFailure() {
        authFailureCounter.increment();
    }

    public void incrementCacheHits() {
        cacheHitCounter.increment();
    }

    public void incrementCacheMisses() {
        cacheMissCounter.increment();
    }

    public void incrementEmailsSent() {
        emailsSentCounter.increment();
    }

    public void incrementEmailsFailed() {
        emailsFailedCounter.increment();
    }

    // Gauge Methods
    public void setActiveUserSessions(int count) {
        activeUserSessions.set(count);
    }

    public void incrementActiveUserSessions() {
        activeUserSessions.incrementAndGet();
    }

    public void decrementActiveUserSessions() {
        activeUserSessions.decrementAndGet();
    }

    public void setEmailQueueSize(int size) {
        emailQueueSize.set(size);
    }

    public void addRevenue(long amountInCents) {
        totalRevenue.addAndGet(amountInCents);
    }

    public void setDatabaseConnectionPoolSize(int size) {
        databaseConnectionPoolSize.set(size);
    }

    // Timer Getters
    public Timer getJwtValidationTimer() {
        return jwtValidationTimer;
    }

    public Timer getDatabaseQueryTimer() {
        return databaseQueryTimer;
    }

    public Timer getCacheOperationTimer() {
        return cacheOperationTimer;
    }

    public Timer getEmailSendTimer() {
        return emailSendTimer;
    }

    public Timer getPaymentProcessingTimer() {
        return paymentProcessingTimer;
    }

    // Utility Methods
    public double getCacheHitRatio() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        return total > 0 ? hits / total : 0.0;
    }

    public double getPaymentSuccessRate() {
        double success = paymentsSuccessCounter.count();
        double failure = paymentsFailureCounter.count();
        double total = success + failure;
        return total > 0 ? success / total : 0.0;
    }

    public double getAuthSuccessRate() {
        double success = authSuccessCounter.count();
        double failure = authFailureCounter.count();
        double total = success + failure;
        return total > 0 ? success / total : 0.0;
    }
}
