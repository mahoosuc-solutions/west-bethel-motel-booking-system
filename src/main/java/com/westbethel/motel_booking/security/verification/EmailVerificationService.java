package com.westbethel.motel_booking.security.verification;

import com.westbethel.motel_booking.common.audit.AuditEntry;
import com.westbethel.motel_booking.common.service.AuditService;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling email verification operations with rate limiting.
 *
 * @author Security Agent 1 - Phase 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int MAX_RESEND_PER_HOUR = 3;
    private static final String RATE_LIMIT_KEY_PREFIX = "email_verification:rate_limit:";

    /**
     * Send verification email to a user.
     * Rate limited to MAX_RESEND_PER_HOUR per user.
     *
     * @param user the user
     * @return the generated token
     * @throws IllegalStateException if rate limit exceeded or email already verified
     */
    @Transactional
    public String sendVerificationEmail(User user) {
        log.info("Email verification requested for user: {}", user.getUsername());

        // Check if already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email already verified");
        }

        // Check rate limit
        if (isRateLimited(user.getId())) {
            log.warn("Email verification rate limit exceeded for user: {}", user.getUsername());
            auditService.record(AuditEntry.builder()
                    .id(UUID.randomUUID())
                    .entityType("SECURITY_EVENT")
                    .entityId(user.getUsername())
                    .action("EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED")
                    .performedBy(user.getUsername())
                    .details("Rate limit exceeded for email verification")
                    .occurredAt(OffsetDateTime.now())
                    .build());
            throw new IllegalStateException("Too many verification email requests. Please try again later.");
        }

        // Generate secure token
        String token = UUID.randomUUID().toString();

        // Create verification token
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .verified(false)
                .build();

        verificationRepository.save(verificationToken);

        // Increment rate limit counter
        incrementRateLimit(user.getId());

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(user.getUsername())
                .action("EMAIL_VERIFICATION_SENT")
                .performedBy(user.getUsername())
                .details("Email verification token sent")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("Email verification token generated for user: {}", user.getUsername());

        return token;
    }

    /**
     * Verify user's email using a valid token.
     *
     * @param token the verification token
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("Email verification attempt with token");

        // Find and validate token
        EmailVerificationToken verificationToken = verificationRepository
                .findValidToken(token, OffsetDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Invalid or expired email verification token used");
                    return new IllegalArgumentException("Invalid or expired verification token");
                });

        User user = verificationToken.getUser();

        // Check if already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email already verified");
        }

        // Update user
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Mark token as verified
        verificationToken.markAsVerified();
        verificationRepository.save(verificationToken);

        // Audit log
        auditService.record(AuditEntry.builder()
                .id(UUID.randomUUID())
                .entityType("SECURITY_EVENT")
                .entityId(user.getUsername())
                .action("EMAIL_VERIFIED")
                .performedBy(user.getUsername())
                .details("Email successfully verified")
                .occurredAt(OffsetDateTime.now())
                .build());

        log.info("Email verified for user: {}", user.getUsername());
    }

    /**
     * Resend verification email to a user.
     *
     * @param username the username
     * @return the new token
     * @throws IllegalArgumentException if user not found
     * @throws IllegalStateException if rate limit exceeded or already verified
     */
    @Transactional
    public String resendVerification(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return sendVerificationEmail(user);
    }

    /**
     * Clean up expired tokens.
     * Should be called periodically by a scheduled task.
     *
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupExpired() {
        log.debug("Cleaning up expired email verification tokens");
        int deleted = verificationRepository.deleteExpiredTokens(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired email verification tokens", deleted);
        }
        return deleted;
    }

    /**
     * Check if user is rate limited for verification email requests.
     *
     * @param userId the user ID
     * @return true if rate limited
     */
    private boolean isRateLimited(UUID userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId.toString();
        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            return false;
        }

        try {
            int requestCount = Integer.parseInt(count);
            return requestCount >= MAX_RESEND_PER_HOUR;
        } catch (NumberFormatException e) {
            log.error("Invalid rate limit counter value for user: {}", userId);
            return false;
        }
    }

    /**
     * Increment rate limit counter for a user.
     *
     * @param userId the user ID
     */
    private void incrementRateLimit(UUID userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId.toString();
        Long newCount = redisTemplate.opsForValue().increment(key);

        // Set expiry on first request
        if (newCount != null && newCount == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
    }
}
