package com.westbethel.motel_booking.performance;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Performance Interceptor
 *
 * Intercepts all HTTP requests to:
 * - Measure request execution time
 * - Log slow requests
 * - Record metrics for monitoring
 * - Track endpoint performance
 *
 * Automatically registers metrics in Micrometer for Prometheus export
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000;
    private static final long WARNING_REQUEST_THRESHOLD_MS = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime == null) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String uri = getSimplifiedUri(request.getRequestURI());
        int status = response.getStatus();

        // Record metrics
        recordRequestMetrics(method, uri, status, duration);

        // Log based on duration
        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("SLOW REQUEST: {} {} - {}ms - Status: {}",
                method, uri, duration, status);
        } else if (duration > WARNING_REQUEST_THRESHOLD_MS) {
            log.info("Request: {} {} - {}ms - Status: {}",
                method, uri, duration, status);
        } else {
            log.debug("Request: {} {} - {}ms - Status: {}",
                method, uri, duration, status);
        }

        // Log errors
        if (ex != null) {
            log.error("Request failed: {} {} - Exception: {}",
                method, uri, ex.getMessage(), ex);
        }
    }

    /**
     * Record request metrics in Micrometer
     */
    private void recordRequestMetrics(String method, String uri, int status, long durationMs) {
        // Record request duration
        Timer.builder("http.server.requests")
            .tag("method", method)
            .tag("uri", uri)
            .tag("status", String.valueOf(status))
            .tag("outcome", getOutcome(status))
            .description("HTTP request duration")
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Record slow requests separately
        if (durationMs > SLOW_REQUEST_THRESHOLD_MS) {
            Timer.builder("http.server.requests.slow")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(status))
                .description("Slow HTTP request duration")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Simplify URI by removing IDs and dynamic segments
     * Converts /api/bookings/123 to /api/bookings/{id}
     */
    private String getSimplifiedUri(String uri) {
        if (uri == null) {
            return "unknown";
        }

        // Remove query parameters
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        // Replace UUIDs with {id}
        uri = uri.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");

        // Replace numeric IDs with {id}
        uri = uri.replaceAll("/\\d+", "/{id}");

        // Limit URI length for cardinality
        if (uri.length() > 100) {
            uri = uri.substring(0, 100);
        }

        return uri;
    }

    /**
     * Determine request outcome based on status code
     */
    private String getOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        } else if (status >= 300 && status < 400) {
            return "REDIRECTION";
        } else if (status >= 400 && status < 500) {
            return "CLIENT_ERROR";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        } else {
            return "UNKNOWN";
        }
    }
}
