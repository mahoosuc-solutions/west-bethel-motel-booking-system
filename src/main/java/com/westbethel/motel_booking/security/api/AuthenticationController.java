package com.westbethel.motel_booking.security.api;

import com.westbethel.motel_booking.security.domain.Role;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.dto.*;
import com.westbethel.motel_booking.security.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * Handles user registration, login, logout, token refresh, and current user information.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return authentication response with tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());

        try {
            // Register user
            User user = authenticationService.register(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );

            // Automatically login after registration
            Map<String, Object> loginResponse = authenticationService.login(
                    request.getUsername(),
                    request.getPassword()
            );

            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken((String) loginResponse.get("accessToken"))
                    .refreshToken((String) loginResponse.get("refreshToken"))
                    .tokenType((String) loginResponse.get("tokenType"))
                    .expiresIn((Long) loginResponse.get("expiresIn"))
                    .username((String) loginResponse.get("username"))
                    .roles((java.util.List<String>) loginResponse.get("roles"))
                    .build();

            log.info("User registered and logged in successfully: {}", request.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Login with username/email and password.
     *
     * @param request login credentials
     * @return authentication response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for user: {}", request.getUsername());

        Map<String, Object> loginResponse = authenticationService.login(
                request.getUsername(),
                request.getPassword()
        );

        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken((String) loginResponse.get("accessToken"))
                .refreshToken((String) loginResponse.get("refreshToken"))
                .tokenType((String) loginResponse.get("tokenType"))
                .expiresIn((Long) loginResponse.get("expiresIn"))
                .username((String) loginResponse.get("username"))
                .roles((java.util.List<String>) loginResponse.get("roles"))
                .build();

        log.info("User logged in successfully: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Logout current user.
     *
     * @return success response
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            // Extract token from Authorization header
            String token = null;
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            // Extract IP address from request
            String ipAddress = request.getRemoteAddr();

            authenticationService.logout(username, token, ipAddress);
            log.info("User logged out: {}", username);
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token
     * @return new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request");

        Map<String, Object> refreshResponse = authenticationService.refreshToken(request.getRefreshToken());

        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken((String) refreshResponse.get("accessToken"))
                .tokenType((String) refreshResponse.get("tokenType"))
                .expiresIn((Long) refreshResponse.get("expiresIn"))
                .username((String) refreshResponse.get("username"))
                .build();

        log.debug("Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user information.
     *
     * @return user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = authentication.getName();
        User user = authenticationService.getCurrentUser(username);

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for IllegalArgumentException.
     *
     * @param e exception
     * @return error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * Exception handler for general exceptions.
     *
     * @param e exception
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Unexpected error in authentication controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
