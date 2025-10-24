package com.westbethel.motel_booking.security.integration;

import com.westbethel.motel_booking.security.util.InjectionPayloadProvider;
import com.westbethel.motel_booking.security.util.SecurityTestUtils;
import com.westbethel.motel_booking.testutil.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for injection attack prevention.
 * Tests all major endpoints against SQL injection, XSS, path traversal,
 * command injection, and other attack vectors.
 */
class InjectionSecurityTest extends BaseIntegrationTest {

    // ==================== SQL Injection Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getSqlInjectionPayloads")
    void testGuestCreationRejectsSqlInjectionInEmail(String sqlPayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            sqlPayload + "@example.com",
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getSqlInjectionPayloads")
    void testGuestCreationRejectsSqlInjectionInAddress(String sqlPayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );

        @SuppressWarnings("unchecked")
        Map<String, String> address = (Map<String, String>) guest.get("address");
        address.put("city", sqlPayload);

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getSqlInjectionPayloads")
    void testPaymentRejectsSqlInjectionInCurrency(String sqlPayload) throws Exception {
        Map<String, Object> payment = SecurityTestUtils.createTestPayment("100.00", sqlPayload);

        mockMvc.perform(SecurityTestUtils.postJson(
                "/api/payments/authorize/" + UUID.randomUUID(), payment))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSqlInjectionInQueryParameters() throws Exception {
        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            // Test availability query with SQL injection in date parameter
            mockMvc.perform(get("/api/availability/search")
                    .param("propertyId", UUID.randomUUID().toString())
                    .param("checkIn", payload)
                    .param("checkOut", "2025-12-05")
                    .param("adults", "2")
                    .param("children", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testSqlInjectionInPathVariable() throws Exception {
        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            mockMvc.perform(get("/api/guests/" + payload))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== XSS Injection Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getXssPayloads")
    void testGuestCreationRejectsXssInEmail(String xssPayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            xssPayload,
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getXssPayloads")
    void testGuestCreationRejectsXssInName(String xssPayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );
        guest.put("preferences", xssPayload);

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getXssPayloads")
    void testXssInQueryParameters(String xssPayload) throws Exception {
        mockMvc.perform(get("/api/availability/search")
                .param("propertyId", UUID.randomUUID().toString())
                .param("checkIn", "2025-12-01")
                .param("checkOut", "2025-12-05")
                .param("adults", xssPayload)
                .param("children", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testXssNotReflectedInErrorMessages() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(xssPayload, "+1-234-567-8900");

        String response = mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Ensure XSS payload is not reflected in error message
        assertThat(response).doesNotContain("<script>");
        assertThat(response).doesNotContain("alert('XSS')");
    }

    // ==================== Path Traversal Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getPathTraversalPayloads")
    void testPathTraversalInPathVariable(String pathPayload) throws Exception {
        mockMvc.perform(get("/api/guests/" + pathPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPathTraversalInFileUpload() throws Exception {
        for (String payload : InjectionPayloadProvider.getPathTraversalPayloads()) {
            // If file upload is supported, test it here
            // This is a placeholder for file upload testing
        }
    }

    // ==================== Command Injection Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getCommandInjectionPayloads")
    void testCommandInjectionInGuestData(String cmdPayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "+1-234-567-8900"
        );
        guest.put("preferences", cmdPayload);

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    // ==================== LDAP Injection Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getLdapInjectionPayloads")
    void testLdapInjectionInSearchParameters(String ldapPayload) throws Exception {
        // If LDAP is used, test search parameters
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            ldapPayload,
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    // ==================== JSON Injection Tests ====================

    @Test
    void testJsonInjectionAttempts() throws Exception {
        // Test injecting additional fields through JSON
        String maliciousJson = "{\"email\":\"test@example.com\",\"phone\":\"+1-234-567-8900\"," +
                "\"role\":\"admin\",\"isAdmin\":true}";

        mockMvc.perform(post("/api/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testJsonStructureManipulation() throws Exception {
        // Test nested JSON injection
        String nestedInjection = "{\"contactDetails\":{\"email\":\"test@example.com\"," +
                "\"phone\":\"+1-234-567-8900\"},\"hidden\":{\"admin\":true}}";

        mockMvc.perform(post("/api/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(nestedInjection))
                .andExpect(status().isBadRequest());
    }

    // ==================== Null Byte Injection Tests ====================

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getNullBytePayloads")
    void testNullByteInjection(String nullBytePayload) throws Exception {
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            nullBytePayload,
            "+1-234-567-8900"
        );

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    // ==================== Mass Assignment Protection Tests ====================

    @Test
    void testMassAssignmentProtection() throws Exception {
        // Attempt to set internal/protected fields
        String maliciousJson = "{" +
                "\"id\":\"" + UUID.randomUUID() + "\"," +
                "\"createdAt\":\"2020-01-01T00:00:00\"," +
                "\"updatedAt\":\"2020-01-01T00:00:00\"," +
                "\"contactDetails\":{\"email\":\"test@example.com\",\"phone\":\"+1-234-567-8900\"}," +
                "\"address\":{\"line1\":\"123 Test St\",\"city\":\"Test\",\"state\":\"TS\"," +
                "\"postalCode\":\"12345\",\"country\":\"USA\"}," +
                "\"marketingOptIn\":false" +
                "}";

        mockMvc.perform(post("/api/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousJson))
                .andExpect(status().isBadRequest());
    }

    // ==================== Overflow Attack Tests ====================

    @Test
    void testLargePayloadRejection() throws Exception {
        // Test with extremely large input
        String largeEmail = InjectionPayloadProvider.getLargePayload(10000) + "@example.com";
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(largeEmail, "+1-234-567-8900");

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testVeryLargeRequestBodyRejection() throws Exception {
        // Test request size limits
        String largeJson = "{\"preferences\":\"" + InjectionPayloadProvider.getLargePayload(15000000) + "\"}";

        mockMvc.perform(post("/api/guests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(largeJson))
                .andExpect(status().isPayloadTooLarge());
    }

    // ==================== Multiple Injection Vectors Tests ====================

    @Test
    void testMultipleInjectionVectorsSimultaneously() throws Exception {
        // Combine multiple attack vectors
        Map<String, Object> guest = SecurityTestUtils.createTestGuest(
            "'; DROP TABLE users--@example.com",
            "+1-234-567-8900"
        );

        @SuppressWarnings("unchecked")
        Map<String, String> address = (Map<String, String>) guest.get("address");
        address.put("city", "<script>alert('XSS')</script>");
        address.put("line1", "../../../etc/passwd");
        guest.put("preferences", "; rm -rf /");

        mockMvc.perform(SecurityTestUtils.postJson("/api/guests", guest))
                .andExpect(status().isBadRequest());
    }

    // ==================== Response Security Tests ====================

    @Test
    void testNoSensitiveDataInErrorResponse() throws Exception {
        Map<String, Object> invalidGuest = SecurityTestUtils.createTestGuest(
            "invalid-email",
            "invalid-phone"
        );

        String response = mockMvc.perform(SecurityTestUtils.postJson("/api/guests", invalidGuest))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Ensure no sensitive data is exposed
        assertThat(response).doesNotContain("password");
        assertThat(response).doesNotContain("secret");
        assertThat(response).doesNotContain("token");
        assertThat(response).doesNotContainIgnoringCase("database");
        assertThat(response).doesNotContainIgnoringCase("jdbc");
    }

    @Test
    void testNoStackTraceInErrorResponse() throws Exception {
        Map<String, Object> invalidGuest = SecurityTestUtils.createTestGuest(
            "test@example.com",
            "invalid-phone"
        );

        String response = mockMvc.perform(SecurityTestUtils.postJson("/api/guests", invalidGuest))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Ensure no stack trace is exposed
        assertThat(response).doesNotContain("at java.");
        assertThat(response).doesNotContain("Caused by:");
        assertThat(response).doesNotContain(".java:");
    }

    // Helper method to avoid compilation error
    private static org.assertj.core.api.AbstractStringAssert<?> assertThat(String actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
