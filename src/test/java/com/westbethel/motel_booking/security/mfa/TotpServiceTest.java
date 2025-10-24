package com.westbethel.motel_booking.security.mfa;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TotpService.
 *
 * @author Security Agent 1 - Phase 2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TOTP Service Tests")
class TotpServiceTest {

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService(googleAuthenticator);
        ReflectionTestUtils.setField(totpService, "appName", "Test App");
    }

    @Test
    @DisplayName("Should generate secret successfully")
    void generateSecret_ReturnsValidSecret() {
        // Arrange
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder("TESTSECRET123").build();
        when(googleAuthenticator.createCredentials()).thenReturn(key);

        // Act
        String secret = totpService.generateSecret();

        // Assert
        assertNotNull(secret);
        assertEquals("TESTSECRET123", secret);
        verify(googleAuthenticator, times(1)).createCredentials();
    }

    @Test
    @DisplayName("Should generate QR code URL in correct format")
    void generateQrCodeUrl_ReturnsValidUrl() {
        // Arrange
        String username = "testuser";
        String secret = "TESTSECRET123";

        // Act
        String qrCodeUrl = totpService.generateQrCodeUrl(username, secret);

        // Assert
        assertNotNull(qrCodeUrl);
        assertTrue(qrCodeUrl.startsWith("otpauth://totp/"));
        assertTrue(qrCodeUrl.contains(username));
        assertTrue(qrCodeUrl.contains("secret=" + secret));
    }

    @Test
    @DisplayName("Should verify valid TOTP code")
    void verifyCode_ValidCode_ReturnsTrue() {
        // Arrange
        String secret = "TESTSECRET123";
        String code = "123456";
        when(googleAuthenticator.authorize(secret, 123456)).thenReturn(true);

        // Act
        boolean result = totpService.verifyCode(secret, code);

        // Assert
        assertTrue(result);
        verify(googleAuthenticator, times(1)).authorize(secret, 123456);
    }

    @Test
    @DisplayName("Should reject invalid TOTP code")
    void verifyCode_InvalidCode_ReturnsFalse() {
        // Arrange
        String secret = "TESTSECRET123";
        String code = "999999";
        when(googleAuthenticator.authorize(secret, 999999)).thenReturn(false);

        // Act
        boolean result = totpService.verifyCode(secret, code);

        // Assert
        assertFalse(result);
        verify(googleAuthenticator, times(1)).authorize(secret, 999999);
    }

    @Test
    @DisplayName("Should reject non-numeric code")
    void verifyCode_NonNumericCode_ReturnsFalse() {
        // Arrange
        String secret = "TESTSECRET123";
        String code = "ABCDEF";

        // Act
        boolean result = totpService.verifyCode(secret, code);

        // Assert
        assertFalse(result);
        verify(googleAuthenticator, never()).authorize(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should generate correct number of backup codes")
    void generateBackupCodes_ReturnsCorrectCount() {
        // Act
        List<String> backupCodes = totpService.generateBackupCodes();

        // Assert
        assertNotNull(backupCodes);
        assertEquals(10, backupCodes.size());
    }

    @Test
    @DisplayName("Should generate backup codes with correct length")
    void generateBackupCodes_CodesHaveCorrectLength() {
        // Act
        List<String> backupCodes = totpService.generateBackupCodes();

        // Assert
        for (String code : backupCodes) {
            assertEquals(8, code.length());
            assertTrue(code.matches("^[A-Z0-9]+$"));
        }
    }

    @Test
    @DisplayName("Should generate unique backup codes")
    void generateBackupCodes_CodesAreUnique() {
        // Act
        List<String> backupCodes = totpService.generateBackupCodes();

        // Assert
        long uniqueCount = backupCodes.stream().distinct().count();
        assertEquals(backupCodes.size(), uniqueCount);
    }

    @Test
    @DisplayName("Should format backup code correctly")
    void formatBackupCode_AddsHyphen() {
        // Arrange
        String code = "ABCD1234";

        // Act
        String formatted = totpService.formatBackupCode(code);

        // Assert
        assertEquals("ABCD-1234", formatted);
    }

    @Test
    @DisplayName("Should not format incorrect length code")
    void formatBackupCode_IncorrectLength_ReturnsOriginal() {
        // Arrange
        String code = "ABC";

        // Act
        String formatted = totpService.formatBackupCode(code);

        // Assert
        assertEquals(code, formatted);
    }
}
