package com.westbethel.motel_booking.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Performance Monitor Tests
 *
 * Validates:
 * - Query tracking
 * - Cache operation tracking
 * - Request tracking
 * - Custom metrics
 */
class PerformanceMonitorTest {

    private PerformanceMonitor performanceMonitor;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        performanceMonitor = new PerformanceMonitor(meterRegistry);
    }

    @Test
    void testTrackQuery() {
        String result = performanceMonitor.trackQuery("test-query", () -> "query-result");

        assertThat(result).isEqualTo("query-result");
        assertThat(meterRegistry.find("db.query.duration").timer()).isNotNull();
    }

    @Test
    void testTrackQueryWithException() {
        assertThatThrownBy(() ->
            performanceMonitor.trackQuery("failing-query", () -> {
                throw new RuntimeException("Query failed");
            })
        ).isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.find("db.query.errors").counter()).isNotNull();
    }

    @Test
    void testTrackCacheOperation() {
        String result = performanceMonitor.trackCacheOperation(
            "test-cache",
            "get",
            () -> "cached-value"
        );

        assertThat(result).isEqualTo("cached-value");
        assertThat(meterRegistry.find("cache.operation.duration").timer()).isNotNull();
    }

    @Test
    void testRecordCacheHit() {
        performanceMonitor.recordCacheHit("test-cache");

        var counter = meterRegistry.find("cache.hits").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testRecordCacheMiss() {
        performanceMonitor.recordCacheMiss("test-cache");

        var counter = meterRegistry.find("cache.misses").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testTrackRequest() {
        String result = performanceMonitor.trackRequest(
            "/api/bookings",
            "GET",
            () -> "request-result"
        );

        assertThat(result).isEqualTo("request-result");
        assertThat(meterRegistry.find("http.request.duration").timer()).isNotNull();
    }

    @Test
    void testTrackRequestWithException() {
        assertThatThrownBy(() ->
            performanceMonitor.trackRequest("/api/bookings", "POST", () -> {
                throw new RuntimeException("Request failed");
            })
        ).isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.find("http.request.errors").counter()).isNotNull();
    }

    @Test
    void testRecordMetric() {
        performanceMonitor.recordMetric("custom.metric", 42.0, "tag1", "value1", "tag2", "value2");

        var counter = meterRegistry.find("custom.metric").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(42.0);
    }

    @Test
    void testTrackOperation() {
        String result = performanceMonitor.trackOperation("custom-operation", () -> "operation-result");

        assertThat(result).isEqualTo("operation-result");
        assertThat(meterRegistry.find("operation.duration").timer()).isNotNull();
    }

    @Test
    void testRecordDuration() {
        performanceMonitor.recordDuration("custom.duration", Duration.ofMillis(100), "operation", "test");

        var timer = meterRegistry.find("custom.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void testMultipleCacheHits() {
        for (int i = 0; i < 10; i++) {
            performanceMonitor.recordCacheHit("test-cache");
        }

        var counter = meterRegistry.find("cache.hits").counter();
        assertThat(counter.count()).isEqualTo(10.0);
    }

    @Test
    void testSlowQueryDetection() {
        // Simulate slow query
        performanceMonitor.trackQuery("slow-query", () -> {
            try {
                Thread.sleep(1100); // > 1000ms threshold
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "result";
        });

        // Should record both normal and slow query metrics
        assertThat(meterRegistry.find("db.query.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("db.query.slow").counter()).isNotNull();
    }
}
