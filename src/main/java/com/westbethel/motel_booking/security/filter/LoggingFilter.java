package com.westbethel.motel_booking.security.filter;

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
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Filter for logging all incoming HTTP requests and responses.
 * Adds correlation IDs for request tracking and logs request details.
 * Does NOT log sensitive data like passwords, tokens, or credit card information.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    // Sensitive headers that should not be logged
    private static final String[] SENSITIVE_HEADERS = {
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token"
    };

    // Sensitive path patterns that should have minimal logging
    private static final String[] SENSITIVE_PATHS = {
            "/api/v1/auth",
            "/api/v1/payments"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Get or create correlation ID
        String correlationId = getOrCreateCorrelationId(httpRequest);

        // Add correlation ID to MDC for all log statements in this request
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        // Add correlation ID to response header
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            // Log request
            logRequest(httpRequest, correlationId);

            // Continue with the filter chain
            chain.doFilter(request, response);

            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, correlationId, duration);

        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    @Override
    public void destroy() {
        logger.info("LoggingFilter destroyed");
    }

    /**
     * Get correlation ID from header or create a new one
     */
    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Log incoming request details
     */
    private void logRequest(HttpServletRequest request, String correlationId) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String remoteAddr = getClientIpAddress(request);

        if (isSensitivePath(path)) {
            // Minimal logging for sensitive paths
            logger.info("Incoming request: {} {} from {}", method, path, remoteAddr);
        } else {
            // Detailed logging for non-sensitive paths
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Incoming request: ")
                    .append(method)
                    .append(" ")
                    .append(path);

            if (queryString != null && !queryString.isEmpty()) {
                logMessage.append("?").append(sanitizeQueryString(queryString));
            }

            logMessage.append(" from ").append(remoteAddr);

            logger.info(logMessage.toString());

            // Log headers (excluding sensitive ones)
            if (logger.isDebugEnabled()) {
                logHeaders(request);
            }
        }
    }

    /**
     * Log response details
     */
    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                             String correlationId, long duration) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        if (status >= 400) {
            logger.warn("Response: {} {} returned status {} in {}ms",
                    method, path, status, duration);
        } else {
            logger.info("Response: {} {} returned status {} in {}ms",
                    method, path, status, duration);
        }
    }

    /**
     * Log request headers (excluding sensitive ones)
     */
    private void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isSensitiveHeader(headerName)) {
                String headerValue = request.getHeader(headerName);
                logger.debug("Header: {} = {}", headerName, headerValue);
            }
        }
    }

    /**
     * Check if a header is sensitive and should not be logged
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        for (String sensitive : SENSITIVE_HEADERS) {
            if (lowerHeaderName.equals(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a path is sensitive and should have minimal logging
     */
    private boolean isSensitivePath(String path) {
        for (String sensitivePath : SENSITIVE_PATHS) {
            if (path.startsWith(sensitivePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitize query string to remove sensitive data
     */
    private String sanitizeQueryString(String queryString) {
        if (queryString == null) {
            return "";
        }

        String lowerQuery = queryString.toLowerCase();
        if (lowerQuery.contains("password") ||
            lowerQuery.contains("token") ||
            lowerQuery.contains("secret") ||
            lowerQuery.contains("card")) {
            return "[sanitized]";
        }

        return queryString;
    }

    /**
     * Get the client's IP address, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
