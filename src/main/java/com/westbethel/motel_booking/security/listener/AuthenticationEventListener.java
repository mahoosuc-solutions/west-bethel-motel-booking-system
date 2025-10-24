package com.westbethel.motel_booking.security.listener;

import com.westbethel.motel_booking.common.audit.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener for Spring Security authentication events.
 * Automatically audits authentication-related events.
 */
@Component
public class AuthenticationEventListener {

    private final SecurityAuditService securityAuditService;

    public AuthenticationEventListener(SecurityAuditService securityAuditService) {
        this.securityAuditService = securityAuditService;
    }

    /**
     * Listen for successful authentication events
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();

        securityAuditService.auditSuccessfulLogin(username, ipAddress);
    }

    /**
     * Listen for authentication failure events
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication() != null ?
                event.getAuthentication().getName() : "UNKNOWN";
        String ipAddress = getClientIpAddress();
        String reason = event.getException() != null ?
                event.getException().getClass().getSimpleName() : "UNKNOWN";

        securityAuditService.auditFailedLogin(username, ipAddress, reason);
    }

    /**
     * Listen for logout events
     */
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        String username = event.getAuthentication() != null ?
                event.getAuthentication().getName() : "UNKNOWN";
        String ipAddress = getClientIpAddress();

        securityAuditService.auditLogout(username, ipAddress);
    }

    /**
     * Get client IP address from current request
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

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
        } catch (Exception e) {
            // Ignore - return unknown
        }

        return "UNKNOWN";
    }
}
