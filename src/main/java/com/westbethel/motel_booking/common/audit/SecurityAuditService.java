package com.westbethel.motel_booking.common.audit;

import com.westbethel.motel_booking.common.audit.repository.AuditEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for auditing security-related events.
 * Logs authentication, authorization, and sensitive operations.
 * Does NOT log sensitive data like passwords, tokens, or credit card information.
 */
@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private final AuditEntryRepository auditEntryRepository;

    public SecurityAuditService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Audit successful login
     */
    public void auditSuccessfulLogin(String username, String ipAddress) {
        String details = String.format("Successful login from IP: %s", sanitizeIpAddress(ipAddress));
        auditSecurityEvent("AUTHENTICATION", username, "LOGIN_SUCCESS", details);
        logger.info("Successful login: username={}, ip={}", sanitizeUsername(username), sanitizeIpAddress(ipAddress));
    }

    /**
     * Audit failed login attempt
     */
    public void auditFailedLogin(String username, String ipAddress, String reason) {
        String details = String.format("Failed login from IP: %s, reason: %s",
                sanitizeIpAddress(ipAddress), sanitizeReason(reason));
        auditSecurityEvent("AUTHENTICATION", username, "LOGIN_FAILED", details);
        logger.warn("Failed login attempt: username={}, ip={}, reason={}",
                sanitizeUsername(username), sanitizeIpAddress(ipAddress), sanitizeReason(reason));
    }

    /**
     * Audit logout
     */
    public void auditLogout(String username, String ipAddress) {
        String details = String.format("Logout from IP: %s", sanitizeIpAddress(ipAddress));
        auditSecurityEvent("AUTHENTICATION", username, "LOGOUT", details);
        logger.info("Logout: username={}, ip={}", sanitizeUsername(username), sanitizeIpAddress(ipAddress));
    }

    /**
     * Audit access denied event
     */
    public void auditAccessDenied(String username, String resource, String action) {
        String details = String.format("Access denied to %s for action: %s", resource, action);
        auditSecurityEvent("AUTHORIZATION", username, "ACCESS_DENIED", details);
        logger.warn("Access denied: username={}, resource={}, action={}",
                sanitizeUsername(username), resource, action);
    }

    /**
     * Audit payment operation
     */
    public void auditPaymentOperation(String operation, String paymentId, String performedBy, String status) {
        String details = String.format("Payment %s: status=%s", operation, status);
        auditSecurityEvent("PAYMENT", paymentId, operation, details);
        logger.info("Payment operation: operation={}, paymentId={}, performedBy={}, status={}",
                operation, sanitizeId(paymentId), sanitizeUsername(performedBy), status);
    }

    /**
     * Audit booking operation
     */
    public void auditBookingOperation(String operation, String bookingId, String performedBy) {
        String details = String.format("Booking %s by %s", operation, performedBy);
        auditSecurityEvent("BOOKING", bookingId, operation, details);
        logger.info("Booking operation: operation={}, bookingId={}, performedBy={}",
                operation, sanitizeId(bookingId), sanitizeUsername(performedBy));
    }

    /**
     * Audit sensitive data access
     */
    public void auditSensitiveDataAccess(String dataType, String dataId, String performedBy, String action) {
        String details = String.format("Accessed %s data: %s", dataType, action);
        auditSecurityEvent("DATA_ACCESS", dataId, action, details);
        logger.info("Sensitive data access: dataType={}, dataId={}, performedBy={}, action={}",
                dataType, sanitizeId(dataId), sanitizeUsername(performedBy), action);
    }

    /**
     * Audit password change
     */
    public void auditPasswordChange(String username, boolean successful) {
        String action = successful ? "PASSWORD_CHANGE_SUCCESS" : "PASSWORD_CHANGE_FAILED";
        String details = successful ? "Password changed successfully" : "Password change failed";
        auditSecurityEvent("AUTHENTICATION", username, action, details);
        logger.info("Password change: username={}, successful={}", sanitizeUsername(username), successful);
    }

    /**
     * Audit token refresh
     */
    public void auditTokenRefresh(String username, String ipAddress) {
        String details = String.format("Token refreshed from IP: %s", sanitizeIpAddress(ipAddress));
        auditSecurityEvent("AUTHENTICATION", username, "TOKEN_REFRESH", details);
        logger.info("Token refresh: username={}, ip={}", sanitizeUsername(username), sanitizeIpAddress(ipAddress));
    }

    /**
     * Audit account lockout
     */
    public void auditAccountLockout(String username, String reason) {
        String details = String.format("Account locked: %s", sanitizeReason(reason));
        auditSecurityEvent("AUTHENTICATION", username, "ACCOUNT_LOCKED", details);
        logger.warn("Account lockout: username={}, reason={}", sanitizeUsername(username), sanitizeReason(reason));
    }

    /**
     * Audit suspicious activity
     */
    public void auditSuspiciousActivity(String activityType, String details, String ipAddress) {
        String fullDetails = String.format("%s from IP: %s", details, sanitizeIpAddress(ipAddress));
        auditSecurityEvent("SECURITY", activityType, "SUSPICIOUS_ACTIVITY", fullDetails);
        logger.warn("Suspicious activity: type={}, ip={}, details={}",
                activityType, sanitizeIpAddress(ipAddress), details);
    }

    /**
     * Audit rate limit exceeded
     */
    public void auditRateLimitExceeded(String ipAddress, String path) {
        String details = String.format("Rate limit exceeded for path: %s", path);
        auditSecurityEvent("SECURITY", ipAddress, "RATE_LIMIT_EXCEEDED", details);
        logger.warn("Rate limit exceeded: ip={}, path={}", sanitizeIpAddress(ipAddress), path);
    }

    /**
     * Core method to create and save audit entry
     */
    private void auditSecurityEvent(String entityType, String entityId, String action, String details) {
        try {
            AuditEntry entry = AuditEntry.builder()
                    .id(UUID.randomUUID())
                    .entityType(entityType)
                    .entityId(sanitizeId(entityId))
                    .action(action)
                    .performedBy(getCurrentUser())
                    .details(sanitizeDetails(details))
                    .occurredAt(OffsetDateTime.now())
                    .build();

            auditEntryRepository.save(entry);
        } catch (Exception e) {
            // Log error but don't throw - auditing failure shouldn't break the application
            logger.error("Failed to save audit entry: entityType={}, action={}, error={}",
                    entityType, action, e.getMessage(), e);
        }
    }

    /**
     * Get current user from security context or request
     */
    private String getCurrentUser() {
        try {
            // Try to get from Spring Security context
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }

            // Fallback to request attribute
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String user = request.getHeader("X-User-ID");
                if (user != null) {
                    return user;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine current user", e);
        }

        return "SYSTEM";
    }

    /**
     * Sanitize username to prevent log injection
     */
    private String sanitizeUsername(String username) {
        if (username == null) {
            return "ANONYMOUS";
        }
        return username.replaceAll("[\\r\\n]", "");
    }

    /**
     * Sanitize IP address
     */
    private String sanitizeIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "UNKNOWN";
        }
        return ipAddress.replaceAll("[\\r\\n]", "");
    }

    /**
     * Sanitize ID to prevent log injection
     */
    private String sanitizeId(String id) {
        if (id == null) {
            return "UNKNOWN";
        }
        return id.replaceAll("[\\r\\n]", "").substring(0, Math.min(id.length(), 128));
    }

    /**
     * Sanitize reason message
     */
    private String sanitizeReason(String reason) {
        if (reason == null) {
            return "UNKNOWN";
        }
        return reason.replaceAll("[\\r\\n]", "");
    }

    /**
     * Sanitize details to prevent logging sensitive data
     */
    private String sanitizeDetails(String details) {
        if (details == null) {
            return "";
        }

        String sanitized = details;

        // Remove sensitive patterns
        sanitized = sanitized.replaceAll("password[=:]\\s*\\S+", "password=***");
        sanitized = sanitized.replaceAll("token[=:]\\s*\\S+", "token=***");
        sanitized = sanitized.replaceAll("secret[=:]\\s*\\S+", "secret=***");
        sanitized = sanitized.replaceAll("apikey[=:]\\s*\\S+", "apikey=***");
        sanitized = sanitized.replaceAll("authorization[=:]\\s*\\S+", "authorization=***");

        // Credit card patterns (simple pattern)
        sanitized = sanitized.replaceAll("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", "****-****-****-****");

        // Limit length
        if (sanitized.length() > 2048) {
            sanitized = sanitized.substring(0, 2045) + "...";
        }

        return sanitized;
    }
}
