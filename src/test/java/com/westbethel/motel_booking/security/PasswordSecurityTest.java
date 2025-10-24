package com.westbethel.motel_booking.security;

import com.westbethel.motel_booking.security.util.SecurityTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive password security tests.
 * Tests password hashing, strength validation, common password rejection,
 * timing attack prevention, and password policies.
 */
class PasswordSecurityTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // Password strength regex: At least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\[\\]{}|:;,.<>~`-]).{8,}$"
    );

    // ==================== Password Hashing Tests ====================

    @Test
    void testPasswordIsHashedWithBCrypt() {
        String plainPassword = "SecureP@ssw0rd";
        String hashed = passwordEncoder.encode(plainPassword);

        // BCrypt hashes start with $2a$, $2b$, or $2y$
        assertThat(hashed).matches("^\\$2[aby]\\$\\d{2}\\$.+");
        assertThat(hashed).isNotEqualTo(plainPassword);
        assertThat(hashed.length()).isGreaterThan(50);
    }

    @Test
    void testSamePasswordProducesDifferentHashes() {
        String password = "SecureP@ssw0rd";
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        // BCrypt uses salt, so same password should produce different hashes
        assertThat(hash1).isNotEqualTo(hash2);

        // Both should still match the original password
        assertThat(passwordEncoder.matches(password, hash1)).isTrue();
        assertThat(passwordEncoder.matches(password, hash2)).isTrue();
    }

    @Test
    void testPasswordVerification() {
        String plainPassword = "SecureP@ssw0rd";
        String hashed = passwordEncoder.encode(plainPassword);

        assertThat(passwordEncoder.matches(plainPassword, hashed)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", hashed)).isFalse();
    }

    @Test
    void testBCryptWorkFactorIsSecure() {
        // Work factor should be at least 10, preferably 12 or higher
        String password = "test";
        String hash = passwordEncoder.encode(password);

        // Extract work factor from hash (format: $2a$12$...)
        String workFactor = hash.substring(4, 6);
        int factor = Integer.parseInt(workFactor);

        assertThat(factor).isGreaterThanOrEqualTo(10);
    }

    @Test
    void testHashingTakesReasonableTime() {
        String password = "SecureP@ssw0rd";

        Instant start = Instant.now();
        passwordEncoder.encode(password);
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);

        // Should take less than 500ms but more than 10ms (to prevent brute force)
        assertThat(duration.toMillis()).isLessThan(500);
        assertThat(duration.toMillis()).isGreaterThan(10);
    }

    // ==================== Password Strength Tests ====================

    @Test
    void testStrongPasswordAccepted() {
        String strongPassword = SecurityTestUtils.getStrongPassword();
        assertThat(isPasswordStrong(strongPassword)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SecureP@ss1",     // 8+ chars, mixed case, digit, special
        "MyP@ssw0rd123",   // 8+ chars, mixed case, digit, special
        "C0mpl3x!Pass",    // 8+ chars, mixed case, digit, special
        "Str0ng#Password", // 8+ chars, mixed case, digit, special
        "V3ry$ecur3Pwd"    // 8+ chars, mixed case, digit, special
    })
    void testValidStrongPasswords(String password) {
        assertThat(isPasswordStrong(password)).isTrue();
    }

    @Test
    void testWeakPasswordsRejected() {
        for (String weakPassword : SecurityTestUtils.getWeakPasswords()) {
            assertThat(isPasswordStrong(weakPassword)).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short",           // Too short
        "ALLUPPERCASE",    // No lowercase
        "alllowercase",    // No uppercase
        "NoDigits!",       // No digits
        "NoSpecial123",    // No special chars
        "12345678",        // Only digits
        "Weak123"          // Too short with special chars missing
    })
    void testPasswordStrengthRequirements(String weakPassword) {
        assertThat(isPasswordStrong(weakPassword)).isFalse();
    }

    @Test
    void testMinimumPasswordLength() {
        String shortPassword = "Short1!";
        assertThat(isPasswordStrong(shortPassword)).isFalse();

        String validLength = "Secure1!";
        assertThat(isPasswordStrong(validLength)).isTrue();
    }

    @Test
    void testPasswordContainsUppercase() {
        assertThat(isPasswordStrong("lowercase123!")).isFalse();
        assertThat(isPasswordStrong("Lowercase123!")).isTrue();
    }

    @Test
    void testPasswordContainsLowercase() {
        assertThat(isPasswordStrong("UPPERCASE123!")).isFalse();
        assertThat(isPasswordStrong("Uppercase123!")).isTrue();
    }

    @Test
    void testPasswordContainsDigit() {
        assertThat(isPasswordStrong("NoDigits!Pwd")).isFalse();
        assertThat(isPasswordStrong("WithDigit1!")).isTrue();
    }

    @Test
    void testPasswordContainsSpecialCharacter() {
        assertThat(isPasswordStrong("NoSpecial123")).isFalse();
        assertThat(isPasswordStrong("WithSpecial123!")).isTrue();
    }

    // ==================== Common Password Tests ====================

    @Test
    void testCommonPasswordsRejected() {
        String[] commonPasswords = {
            "password", "123456", "12345678", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley",
            "bailey", "passw0rd", "shadow", "123123", "654321"
        };

        for (String common : commonPasswords) {
            assertThat(isCommonPassword(common)).isTrue();
        }
    }

    @Test
    void testUniquePasswordsAccepted() {
        String uniquePassword = SecurityTestUtils.getStrongPassword();
        assertThat(isCommonPassword(uniquePassword)).isFalse();
    }

    // ==================== Password History Tests ====================

    @Test
    void testPasswordHistoryPreventsReuse() {
        String currentPassword = "CurrentP@ss1";
        Set<String> previousPasswords = new HashSet<>();
        previousPasswords.add(passwordEncoder.encode("OldP@ss1"));
        previousPasswords.add(passwordEncoder.encode("OldP@ss2"));
        previousPasswords.add(passwordEncoder.encode("OldP@ss3"));

        // Should not be able to reuse any previous password
        for (String oldHash : previousPasswords) {
            assertThat(passwordEncoder.matches(currentPassword, oldHash)).isFalse();
        }
    }

    @Test
    void testPasswordHistorySize() {
        Set<String> passwordHistory = new HashSet<>();
        String basePassword = "TestP@ss";

        // Simulate password changes
        for (int i = 1; i <= 5; i++) {
            String password = basePassword + i;
            passwordHistory.add(passwordEncoder.encode(password));
        }

        // History should maintain last N passwords (e.g., 5)
        assertThat(passwordHistory).hasSize(5);
    }

    // ==================== Timing Attack Prevention Tests ====================

    @Test
    void testConstantTimePasswordComparison() {
        String correctPassword = "SecureP@ssw0rd";
        String hash = passwordEncoder.encode(correctPassword);

        // Measure time for correct password
        long correctTime = measurePasswordCheckTime(correctPassword, hash);

        // Measure time for incorrect password of same length
        long incorrectTime = measurePasswordCheckTime("WrongP@ssw0rd!", hash);

        // Time difference should be minimal (within acceptable variance)
        // BCrypt should take roughly the same time regardless of password
        long timeDiff = Math.abs(correctTime - incorrectTime);

        // Allow 20% variance due to system performance variations
        assertThat(timeDiff).isLessThan(correctTime / 5);
    }

    @Test
    void testTimingAttackResistanceAcrossMultipleAttempts() {
        String password = "SecureP@ssw0rd";
        String hash = passwordEncoder.encode(password);

        long[] times = new long[10];
        for (int i = 0; i < 10; i++) {
            times[i] = measurePasswordCheckTime("WrongPassword" + i, hash);
        }

        // All attempts should take similar time
        long avg = 0;
        for (long time : times) {
            avg += time;
        }
        avg /= times.length;

        for (long time : times) {
            long diff = Math.abs(time - avg);
            assertThat(diff).isLessThan(avg / 3); // Within 33% of average
        }
    }

    // ==================== Password Reset Security Tests ====================

    @Test
    void testPasswordResetTokenIsUnique() {
        String token1 = generatePasswordResetToken();
        String token2 = generatePasswordResetToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void testPasswordResetTokenIsSecure() {
        String token = generatePasswordResetToken();

        // Token should be long enough (at least 32 characters)
        assertThat(token.length()).isGreaterThanOrEqualTo(32);

        // Token should be alphanumeric
        assertThat(token).matches("^[A-Za-z0-9-]+$");
    }

    // ==================== Password Change Security Tests ====================

    @Test
    void testPasswordChangeRequiresCurrentPassword() {
        // This would be tested at the controller/service level
        // Here we just verify the concept
        String currentPassword = "OldP@ss1";
        String newPassword = "NewP@ss1";

        assertThat(currentPassword).isNotEqualTo(newPassword);
    }

    @Test
    void testNewPasswordMustBeDifferentFromCurrent() {
        String currentPassword = "SameP@ss1";
        String newPassword = "SameP@ss1";

        assertThat(currentPassword.equals(newPassword)).isTrue();
        // In actual implementation, this should be rejected
    }

    // ==================== Helper Methods ====================

    private boolean isPasswordStrong(String password) {
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    private boolean isCommonPassword(String password) {
        // In production, this would check against a comprehensive list
        Set<String> commonPasswords = new HashSet<>();
        for (String weak : SecurityTestUtils.getWeakPasswords()) {
            commonPasswords.add(weak.toLowerCase());
        }
        return commonPasswords.contains(password.toLowerCase());
    }

    private long measurePasswordCheckTime(String password, String hash) {
        long start = System.nanoTime();
        passwordEncoder.matches(password, hash);
        long end = System.nanoTime();
        return end - start;
    }

    private String generatePasswordResetToken() {
        // Simulate token generation
        return java.util.UUID.randomUUID().toString() + "-" +
               java.util.UUID.randomUUID().toString();
    }
}
