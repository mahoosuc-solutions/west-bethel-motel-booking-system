package com.westbethel.motel_booking.performance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Performance Monitoring Service
 *
 * Provides utilities for:
 * - Tracking operation execution time
 * - Recording custom metrics
 * - Slow query detection and logging
 * - Cache hit/miss tracking
 *
 * Integrates with Micrometer for metrics export to Prometheus
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitor {

    private final MeterRegistry meterRegistry;

    // Slow query threshold (milliseconds)
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000;

    /**
     * Track execution time of a database query
     *
     * @param queryName Name of the query
     * @param operation Query operation to execute
     * @return Query result
     */
    public <T> T trackQuery(String queryName, Supplier<T> operation) {
        Timer timer = Timer.builder("db.query.duration")
            .tag("query", queryName)
            .description("Database query execution time")
            .register(meterRegistry);

        long startTime = System.currentTimeMillis();
        try {
            T result = timer.record(operation);
            long duration = System.currentTimeMillis() - startTime;

            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("SLOW QUERY DETECTED: {} took {}ms", queryName, duration);
                recordSlowQuery(queryName, duration);
            } else {
                log.debug("Query {} completed in {}ms", queryName, duration);
            }

            return result;
        } catch (Exception e) {
            recordQueryError(queryName);
            throw e;
        }
    }

    /**
     * Track execution time of a cache operation
     *
     * @param cacheName Cache name
     * @param operation Cache operation type (hit/miss/evict)
     * @param action Action to execute
     */
    public <T> T trackCacheOperation(String cacheName, String operation, Supplier<T> action) {
        Timer timer = Timer.builder("cache.operation.duration")
            .tag("cache", cacheName)
            .tag("operation", operation)
            .description("Cache operation execution time")
            .register(meterRegistry);

        return timer.record(action);
    }

    /**
     * Record a cache hit
     *
     * @param cacheName Cache name
     */
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hits")
            .tag("cache", cacheName)
            .description("Cache hit count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a cache miss
     *
     * @param cacheName Cache name
     */
    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.misses")
            .tag("cache", cacheName)
            .description("Cache miss count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a slow query
     *
     * @param queryName Query name
     * @param durationMs Duration in milliseconds
     */
    private void recordSlowQuery(String queryName, long durationMs) {
        Counter.builder("db.query.slow")
            .tag("query", queryName)
            .description("Slow query count")
            .register(meterRegistry)
            .increment();

        Timer.builder("db.query.slow.duration")
            .tag("query", queryName)
            .description("Slow query duration")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record a query error
     *
     * @param queryName Query name
     */
    private void recordQueryError(String queryName) {
        Counter.builder("db.query.errors")
            .tag("query", queryName)
            .description("Query error count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Track HTTP request execution time
     *
     * @param endpoint Endpoint path
     * @param method HTTP method
     * @param operation Request operation
     * @return Operation result
     */
    public <T> T trackRequest(String endpoint, String method, Supplier<T> operation) {
        Timer timer = Timer.builder("http.request.duration")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .description("HTTP request execution time")
            .register(meterRegistry);

        long startTime = System.currentTimeMillis();
        try {
            T result = timer.record(operation);
            long duration = System.currentTimeMillis() - startTime;

            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("SLOW REQUEST DETECTED: {} {} took {}ms", method, endpoint, duration);
                recordSlowRequest(endpoint, method, duration);
            }

            return result;
        } catch (Exception e) {
            recordRequestError(endpoint, method);
            throw e;
        }
    }

    /**
     * Record a slow HTTP request
     *
     * @param endpoint Endpoint path
     * @param method HTTP method
     * @param durationMs Duration in milliseconds
     */
    private void recordSlowRequest(String endpoint, String method, long durationMs) {
        Counter.builder("http.request.slow")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .description("Slow request count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record an HTTP request error
     *
     * @param endpoint Endpoint path
     * @param method HTTP method
     */
    private void recordRequestError(String endpoint, String method) {
        Counter.builder("http.request.errors")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .description("Request error count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record a custom metric
     *
     * @param metricName Metric name
     * @param value Metric value
     * @param tags Optional tags
     */
    public void recordMetric(String metricName, double value, String... tags) {
        var builder = Counter.builder(metricName);

        // Add tags in pairs (key, value)
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(meterRegistry).increment(value);
    }

    /**
     * Track custom operation execution time
     *
     * @param operationName Operation name
     * @param operation Operation to execute
     * @return Operation result
     */
    public <T> T trackOperation(String operationName, Supplier<T> operation) {
        Timer timer = Timer.builder("operation.duration")
            .tag("operation", operationName)
            .description("Custom operation execution time")
            .register(meterRegistry);

        return timer.record(operation);
    }

    /**
     * Record custom duration
     *
     * @param metricName Metric name
     * @param duration Duration
     * @param tags Optional tags
     */
    public void recordDuration(String metricName, Duration duration, String... tags) {
        var builder = Timer.builder(metricName);

        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }

        builder.register(meterRegistry).record(duration);
    }
}
