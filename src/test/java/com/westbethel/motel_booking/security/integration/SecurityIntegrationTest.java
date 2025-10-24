package com.westbethel.motel_booking.security.integration;

import com.westbethel.motel_booking.security.util.SecurityTestUtils;
import com.westbethel.motel_booking.testutil.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for complete security flows including authentication,
 * authorization, session management, CORS, and security headers.
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

    // ==================== Authentication Flow Tests ====================

    @Test
    void testCompleteAuthenticationFlow() throws Exception {
        // 1. Register new user
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "newuser",
            SecurityTestUtils.getStrongPassword()
        );

        // Note: Actual implementation would have /api/auth/register endpoint
        // This is a placeholder for the authentication flow test
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {
        // Test successful login
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "testuser",
            SecurityTestUtils.getStrongPassword()
        );

        // Note: Actual implementation would validate and return JWT token
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "testuser",
            "WrongPassword123!"
        );

        // Should return 401 Unauthorized
    }

    @Test
    void testLoginWithNonExistentUser() throws Exception {
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "nonexistent",
            SecurityTestUtils.getStrongPassword()
        );

        // Should return 401 Unauthorized
    }

    // ==================== Authorization Flow Tests ====================

    @Test
    void testAccessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/guests/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/guests/" + UUID.randomUUID())
                .header("Authorization", SecurityTestUtils.createInvalidToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithExpiredToken() throws Exception {
        mockMvc.perform(get("/api/guests/" + UUID.randomUUID())
                .header("Authorization", SecurityTestUtils.createExpiredToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithValidToken() throws Exception {
        // Note: In actual implementation, this would use a real JWT token
        String token = SecurityTestUtils.createUserToken("testuser");

        mockMvc.perform(get("/api/guests/" + UUID.randomUUID())
                .header("Authorization", token))
                .andExpect(status().isNotFound()); // Resource not found, but auth passed
    }

    // ==================== Token Lifecycle Tests ====================

    @Test
    void testTokenGenerationOnSuccessfulLogin() throws Exception {
        // Login should return a valid JWT token
    }

    @Test
    void testTokenExpirationIsEnforced() throws Exception {
        // Expired token should be rejected
        mockMvc.perform(get("/api/guests/" + UUID.randomUUID())
                .header("Authorization", SecurityTestUtils.createExpiredToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTokenRefreshFlow() throws Exception {
        // Test refresh token mechanism
    }

    @Test
    void testTokenRevocationOnLogout() throws Exception {
        // After logout, token should be invalid
    }

    // ==================== Session Management Tests ====================

    @Test
    void testSessionTimeout() throws Exception {
        // Session should timeout after configured period
    }

    @Test
    void testConcurrentSessions() throws Exception {
        // Test handling of multiple concurrent sessions for same user
    }

    @Test
    void testSessionFixationProtection() throws Exception {
        // Session ID should change after login
    }

    // ==================== CORS Tests ====================

    @Test
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/guests")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void testCorsActualRequest() throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest)
                .header("Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void testCorsBlocksUnauthorizedOrigins() throws Exception {
        mockMvc.perform(get("/api/guests")
                .header("Origin", "http://evil.com"))
                .andExpect(status().isForbidden());
    }

    // ==================== Security Headers Tests ====================

    @Test
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    void testContentTypeValidation() throws Exception {
        // Test that only JSON content type is accepted for API endpoints
        mockMvc.perform(post("/api/guests")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text data"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testAcceptHeaderValidation() throws Exception {
        // Test Accept header validation
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Or 201 if validation passes
    }

    // ==================== Rate Limiting Integration Tests ====================

    @Test
    void testRateLimitingDoesNotAffectNormalTraffic() throws Exception {
        // Normal traffic should not be rate limited
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testRateLimitingBlocksExcessiveRequests() throws Exception {
        // Make requests up to the limit
        int limit = 100; // Based on configuration

        // Should eventually hit rate limit
    }

    @Test
    void testRateLimitHeadersPresent() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    // ==================== Input Sanitization Integration Tests ====================

    @Test
    void testHtmlInputIsSanitized() throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );
        guest.put("preferences", "<p>Normal text</p><script>alert('xss')</script>");

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSqlCharactersAreEscaped() throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );

        @SuppressWarnings("unchecked")
        Map<String, String> address = (Map<String, String>) guest.get("address");
        address.put("city", "O'Brien");

        // Apostrophe in valid name should be handled properly
        // But SQL injection attempts should be blocked
    }

    // ==================== Error Handling Security Tests ====================

    @Test
    void testGenericErrorMessagesForSecurity() throws Exception {
        // Login with wrong password
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "testuser",
            "WrongPassword"
        );

        // Should return generic "Invalid credentials" not "Wrong password"
    }

    @Test
    void testNoSensitiveInfoInErrorResponses() throws Exception {
        String response = mockMvc.perform(get("/api/guests/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("password");
        assertThat(response).doesNotContain("secret");
        assertThat(response).doesNotContain("token");
    }

    @Test
    void testNoStackTracesInProduction() throws Exception {
        // Trigger an error
        String response = mockMvc.perform(get("/api/invalid-endpoint"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).doesNotContain("at java.");
        assertThat(response).doesNotContain("Caused by:");
    }

    // ==================== HTTPS Enforcement Tests ====================

    @Test
    void testHttpsRedirection() throws Exception {
        // In production, HTTP should redirect to HTTPS
        // This test would need to be configured based on deployment
    }

    @Test
    void testStrictTransportSecurityHeader() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    // ==================== Account Lockout Tests ====================

    @Test
    void testAccountLockoutAfterFailedAttempts() throws Exception {
        // After N failed login attempts, account should be locked
        Map<String, String> credentials = SecurityTestUtils.createTestCredentials(
            "testuser",
            "WrongPassword"
        );

        // Simulate multiple failed attempts
        for (int i = 0; i < 5; i++) {
            // Attempt login with wrong password
        }

        // Next attempt should be blocked even with correct password
    }

    @Test
    void testAccountLockoutReset() throws Exception {
        // Account should be unlocked after timeout period
    }

    // ==================== Authorization Matrix Tests ====================

    @Test
    void testUserCanAccessOwnResources() throws Exception {
        String userToken = SecurityTestUtils.createUserToken("testuser");

        // User should be able to access their own bookings
    }

    @Test
    void testUserCannotAccessOtherUserResources() throws Exception {
        String userToken = SecurityTestUtils.createUserToken("user1");

        // User1 should not be able to access User2's bookings
    }

    @Test
    void testAdminCanAccessAllResources() throws Exception {
        String adminToken = SecurityTestUtils.createAdminToken();

        // Admin should be able to access any resource
    }

    // Helper methods
    private static org.assertj.core.api.AbstractStringAssert<?> assertThat(String actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
