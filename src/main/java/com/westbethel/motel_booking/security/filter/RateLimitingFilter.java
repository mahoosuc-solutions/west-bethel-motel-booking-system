package com.westbethel.motel_booking.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.westbethel.motel_booking.common.dto.ErrorResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic rate limiting filter using in-memory storage.
 * Limits the number of requests per IP address per time window.
 * Can be replaced with Redis-based implementation for distributed environments.
 */
@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${security.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    // In-memory storage for rate limiting (IP -> bucket)
    private final Map<String, RateLimitBucket> rateLimitMap = new ConcurrentHashMap<>();

    // Cleanup thread to remove old entries
    private Thread cleanupThread;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (rateLimitEnabled) {
            logger.info("RateLimitingFilter initialized with {} requests per minute", requestsPerMinute);
            startCleanupThread();
        } else {
            logger.info("Rate limiting is disabled");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIpAddress(httpRequest);

        // Get or create rate limit bucket for this IP
        RateLimitBucket bucket = rateLimitMap.computeIfAbsent(clientIp, k -> new RateLimitBucket());

        // Check if request is allowed
        if (bucket.allowRequest()) {
            // Add rate limit headers
            addRateLimitHeaders(httpResponse, bucket);
            chain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            handleRateLimitExceeded(httpRequest, httpResponse, bucket);
        }
    }

    @Override
    public void destroy() {
        if (cleanupThread != null) {
            cleanupThread.interrupt();
        }
        logger.info("RateLimitingFilter destroyed");
    }

    /**
     * Handle rate limit exceeded scenario
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response,
                                        RateLimitBucket bucket) throws IOException {
        String clientIp = getClientIpAddress(request);
        logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getRequestURI());

        // Set response status and headers
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        addRateLimitHeaders(response, bucket);

        // Add Retry-After header
        long resetTimeSeconds = (bucket.getWindowEnd() - System.currentTimeMillis()) / 1000;
        response.setHeader("Retry-After", String.valueOf(Math.max(1, resetTimeSeconds)));

        // Create error response
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .code("RATE_LIMIT_EXCEEDED")
                .message("Too many requests. Please try again later")
                .path(request.getRequestURI())
                .build();

        // Write error response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Add rate limit headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitBucket bucket) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getWindowEnd() / 1000));
    }

    /**
     * Get the client's IP address, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Start background thread to cleanup old rate limit entries
     */
    private void startCleanupThread() {
        cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // Cleanup every minute
                    cleanupExpiredBuckets();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("RateLimitCleanup");
        cleanupThread.start();
    }

    /**
     * Remove expired rate limit buckets
     */
    private void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        AtomicInteger removed = new AtomicInteger(0);

        rateLimitMap.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(now)) {
                removed.incrementAndGet();
                return true;
            }
            return false;
        });

        if (removed.get() > 0) {
            logger.debug("Cleaned up {} expired rate limit buckets", removed.get());
        }
    }

    /**
     * Inner class representing a rate limit bucket for an IP address
     */
    private class RateLimitBucket {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStart;
        private volatile long windowEnd;

        public RateLimitBucket() {
            resetWindow();
        }

        /**
         * Check if request is allowed and increment counter
         */
        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now > windowEnd) {
                resetWindow();
            }

            // Check if under limit
            if (requestCount.get() < requestsPerMinute) {
                requestCount.incrementAndGet();
                return true;
            }

            return false;
        }

        /**
         * Get remaining requests in current window
         */
        public int getRemaining() {
            return Math.max(0, requestsPerMinute - requestCount.get());
        }

        /**
         * Get window end timestamp
         */
        public long getWindowEnd() {
            return windowEnd;
        }

        /**
         * Check if bucket has expired
         */
        public boolean isExpired(long now) {
            return now > windowEnd + 300000; // Expire 5 minutes after window end
        }

        /**
         * Reset the time window
         */
        private void resetWindow() {
            windowStart = System.currentTimeMillis();
            windowEnd = windowStart + 60000; // 1 minute window
            requestCount.set(0);
        }
    }
}
