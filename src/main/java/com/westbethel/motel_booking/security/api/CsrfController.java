package com.westbethel.motel_booking.security.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for CSRF token management.
 * Provides endpoints for clients to retrieve CSRF tokens for state-changing operations.
 */
@RestController
@RequestMapping("/api/v1")
public class CsrfController {

    /**
     * Retrieve the CSRF token.
     * The token is automatically set as a cookie by CookieCsrfTokenRepository.
     * This endpoint also returns the token in the response body and headers for convenience.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return the CSRF token details
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Spring Security automatically sets the CSRF token as a request attribute
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            // If no token exists, create one by accessing it (triggers generation)
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }

        if (csrfToken != null) {
            // Add token to response header for easy access
            response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
            response.setHeader("X-CSRF-HEADER", csrfToken.getHeaderName());
            response.setHeader("X-CSRF-PARAMETER", csrfToken.getParameterName());

            // Return token details in response body
            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("token", csrfToken.getToken());
            tokenData.put("headerName", csrfToken.getHeaderName());
            tokenData.put("parameterName", csrfToken.getParameterName());

            return ResponseEntity.ok(tokenData);
        }

        // Return empty response if token is not available (should not happen)
        return ResponseEntity.ok(new HashMap<>());
    }
}
