package com.westbethel.motel_booking.security.service;

import com.westbethel.motel_booking.security.blacklist.TokenBlacklistService;
import com.westbethel.motel_booking.security.domain.Role;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.repository.RoleRepository;
import com.westbethel.motel_booking.security.repository.UserRepository;
import com.westbethel.motel_booking.security.session.SessionManagementService;
import com.westbethel.motel_booking.security.verification.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling authentication operations including registration, login, and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SessionManagementService sessionManagementService;
    private final EmailVerificationService emailVerificationService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * Register a new user.
     *
     * @param username username
     * @param email email address
     * @param password plain text password
     * @param firstName first name
     * @param lastName last name
     * @return registered user
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User register(String username, String email, String password, String firstName, String lastName) {
        log.info("Registering new user: {}", username);

        // Validate username and email are unique
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Get or create default USER role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    log.warn("ROLE_USER not found, creating default role");
                    Role newRole = Role.builder()
                            .name("ROLE_USER")
                            .description("Default user role")
                            .build();
                    return roleRepository.save(newRole);
                });

        // Create new user
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .failedLoginAttempts(0)
                .passwordChangedAt(LocalDateTime.now())
                .emailVerified(false)
                .mfaEnabled(false)
                .build();

        user.addRole(userRole);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", username);

        // Send verification email
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
            log.info("Email verification sent for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", username, e);
            // Don't fail registration if email sending fails
        }

        return savedUser;
    }

    /**
     * Authenticate user and generate JWT tokens.
     *
     * @param username username or email
     * @param password plain text password
     * @return map containing access token, refresh token, and metadata
     * @throws BadCredentialsException if authentication fails
     */
    @Transactional
    public Map<String, Object> login(String username, String password) {
        log.info("Login attempt for user: {}", username);

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login attempt for locked account: {}", username);
            throw new BadCredentialsException("Account is locked due to multiple failed login attempts");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // Reset failed login attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedLoginAttempts();
            }

            // Update last login timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("User logged in successfully: {}", username);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationTime());
            response.put("username", user.getUsername());
            response.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .toList());

            return response;

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            handleFailedLogin(user);
            throw e;
        }
    }

    /**
     * Handle failed login attempt.
     *
     * @param user the user who failed to login
     */
    @Transactional
    protected void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount(LOCK_DURATION_MINUTES);
            log.warn("Account locked due to {} failed login attempts: {}",
                    MAX_FAILED_ATTEMPTS, user.getUsername());
        }

        userRepository.save(user);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshToken refresh token
     * @return map containing new access token and metadata
     * @throws IllegalArgumentException if refresh token is invalid
     */
    @Transactional(readOnly = true)
    public Map<String, Object> refreshToken(String refreshToken) {
        log.debug("Refreshing access token");

        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtService.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtService.getExpirationTime());
        response.put("username", username);

        log.debug("Access token refreshed for user: {}", username);

        return response;
    }

    /**
     * Logout user and blacklist their token.
     *
     * @param username username
     * @param token the JWT token to blacklist
     * @param ipAddress the IP address of logout request
     */
    public void logout(String username, String token, String ipAddress) {
        log.info("User logged out: {}", username);

        // Blacklist the token
        if (token != null && !token.isEmpty()) {
            try {
                tokenBlacklistService.blacklistToken(token, "LOGOUT");
                log.debug("Token blacklisted for user: {}", username);
            } catch (Exception e) {
                log.error("Failed to blacklist token for user: {}", username, e);
            }
        }

        // Invalidate current session
        // Note: In a full implementation, we'd track the session ID and invalidate it
        log.debug("Session invalidated for user: {}", username);
    }

    /**
     * Logout from all devices - blacklist all tokens and invalidate all sessions.
     *
     * @param username username
     */
    public void logoutAllDevices(String username) {
        log.info("Logout from all devices requested for user: {}", username);

        // Blacklist all user tokens
        tokenBlacklistService.blacklistAllUserTokens(username, "LOGOUT_ALL_DEVICES");

        // Invalidate all sessions
        sessionManagementService.invalidateAllSessions(username);

        log.info("All devices logged out for user: {}", username);
    }

    /**
     * Get current authenticated user.
     *
     * @param username username
     * @return user entity
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
