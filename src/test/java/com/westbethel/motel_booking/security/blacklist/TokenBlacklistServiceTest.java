package com.westbethel.motel_booking.security.blacklist;

import com.westbethel.motel_booking.common.audit.AuditEntry;
import com.westbethel.motel_booking.common.service.AuditService;
import com.westbethel.motel_booking.security.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistService.
 *
 * @author Security Agent 1 - Phase 2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Tests")
class TokenBlacklistServiceTest {

    @Mock
    private TokenBlacklistRepository blacklistRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TokenBlacklistService blacklistService;

    private String testToken;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testToken = "test.jwt.token";
        testUsername = "testuser";
    }

    @Test
    @DisplayName("Should successfully blacklist a valid token")
    void blacklistToken_ValidToken_Success() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);
        when(jwtService.extractExpiration(testToken)).thenReturn(expirationDate);
        when(blacklistRepository.save(any(BlacklistedToken.class))).thenReturn(new BlacklistedToken());

        // Act
        blacklistService.blacklistToken(testToken, "LOGOUT");

        // Assert
        verify(blacklistRepository, times(1)).save(any(BlacklistedToken.class));
        verify(auditService, times(1)).record(argThat(entry ->
                "TOKEN_BLACKLISTED".equals(entry.getAction()) &&
                testUsername.equals(entry.getEntityId())
        ));
    }

    @Test
    @DisplayName("Should not blacklist an already expired token")
    void blacklistToken_ExpiredToken_SkipsBlacklisting() {
        // Arrange
        Date expirationDate = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago
        when(jwtService.extractUsername(testToken)).thenReturn(testUsername);
        when(jwtService.extractExpiration(testToken)).thenReturn(expirationDate);

        // Act
        blacklistService.blacklistToken(testToken, "LOGOUT");

        // Assert
        verify(blacklistRepository, never()).save(any(BlacklistedToken.class));
        verify(auditService, never()).record(any(AuditEntry.class));
    }

    @Test
    @DisplayName("Should correctly identify blacklisted token")
    void isBlacklisted_BlacklistedToken_ReturnsTrue() {
        // Arrange
        when(blacklistRepository.existsById(testToken)).thenReturn(true);

        // Act
        boolean result = blacklistService.isBlacklisted(testToken);

        // Assert
        assertTrue(result);
        verify(blacklistRepository, times(1)).existsById(testToken);
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token")
    void isBlacklisted_NonBlacklistedToken_ReturnsFalse() {
        // Arrange
        when(blacklistRepository.existsById(testToken)).thenReturn(false);

        // Act
        boolean result = blacklistService.isBlacklisted(testToken);

        // Assert
        assertFalse(result);
        verify(blacklistRepository, times(1)).existsById(testToken);
    }

    @Test
    @DisplayName("Should fail closed on Redis error (treat as blacklisted)")
    void isBlacklisted_RedisError_FailsClosed() {
        // Arrange
        when(blacklistRepository.existsById(testToken)).thenThrow(new RuntimeException("Redis connection error"));

        // Act
        boolean result = blacklistService.isBlacklisted(testToken);

        // Assert
        assertTrue(result, "Should fail closed (treat as blacklisted) when Redis is unavailable");
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void blacklistToken_InvalidToken_ThrowsException() {
        // Arrange
        when(jwtService.extractUsername(testToken)).thenThrow(new RuntimeException("Invalid token"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            blacklistService.blacklistToken(testToken, "LOGOUT");
        });
    }

    @Test
    @DisplayName("Should blacklist all user tokens")
    void blacklistAllUserTokens_Success() {
        // Act
        blacklistService.blacklistAllUserTokens(testUsername, "PASSWORD_CHANGE");

        // Assert
        verify(auditService, times(1)).record(argThat(entry ->
                "ALL_TOKENS_BLACKLISTED".equals(entry.getAction()) &&
                testUsername.equals(entry.getEntityId()) &&
                entry.getDetails().contains("PASSWORD_CHANGE")
        ));
    }
}
