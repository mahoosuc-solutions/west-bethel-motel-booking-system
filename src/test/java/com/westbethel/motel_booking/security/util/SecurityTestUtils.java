package com.westbethel.motel_booking.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Utility class for security testing.
 */
public class SecurityTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a mock JWT token for testing.
     */
    public static String createMockJwtToken(String username, String... roles) {
        // In real implementation, this would generate a valid JWT
        // For testing, we use a mock format
        return "Bearer mock.jwt.token." + username + "." + String.join(",", roles);
    }

    /**
     * Create an admin JWT token.
     */
    public static String createAdminToken() {
        return createMockJwtToken("admin", "ROLE_ADMIN");
    }

    /**
     * Create a user JWT token.
     */
    public static String createUserToken(String username) {
        return createMockJwtToken(username, "ROLE_USER");
    }

    /**
     * Create an expired JWT token for testing.
     */
    public static String createExpiredToken() {
        return "Bearer expired.jwt.token";
    }

    /**
     * Create an invalid JWT token for testing.
     */
    public static String createInvalidToken() {
        return "Bearer invalid.format";
    }

    /**
     * Add JWT authorization header to request.
     */
    public static MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", token);
    }

    /**
     * Add admin authorization to request.
     */
    public static MockHttpServletRequestBuilder withAdminAuth(MockHttpServletRequestBuilder request) {
        return withAuth(request, createAdminToken());
    }

    /**
     * Add user authorization to request.
     */
    public static MockHttpServletRequestBuilder withUserAuth(MockHttpServletRequestBuilder request, String username) {
        return withAuth(request, createUserToken(username));
    }

    /**
     * Create a POST request with JSON body.
     */
    public static MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
        return post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Create a PUT request with JSON body.
     */
    public static MockHttpServletRequestBuilder putJson(String url, Object body) throws Exception {
        return put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Create a PATCH request with JSON body.
     */
    public static MockHttpServletRequestBuilder patchJson(String url, Object body) throws Exception {
        return patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Create test guest data.
     */
    public static Map<String, Object> createTestGuest(String email, String phone) {
        Map<String, Object> guest = new HashMap<>();

        Map<String, String> contactDetails = new HashMap<>();
        contactDetails.put("email", email);
        contactDetails.put("phone", phone);
        guest.put("contactDetails", contactDetails);

        Map<String, String> address = new HashMap<>();
        address.put("line1", "123 Test St");
        address.put("city", "Test City");
        address.put("state", "TS");
        address.put("postalCode", "12345");
        address.put("country", "USA");
        guest.put("address", address);

        guest.put("marketingOptIn", false);

        return guest;
    }

    /**
     * Create test booking data.
     */
    public static Map<String, Object> createTestBooking(UUID propertyId, UUID guestId, UUID ratePlanId, UUID roomTypeId) {
        Map<String, Object> booking = new HashMap<>();
        booking.put("propertyId", propertyId.toString());
        booking.put("guestId", guestId.toString());
        booking.put("checkIn", "2025-12-01");
        booking.put("checkOut", "2025-12-05");
        booking.put("adults", 2);
        booking.put("children", 0);
        booking.put("ratePlanId", ratePlanId.toString());
        booking.put("roomTypeIds", new String[]{roomTypeId.toString()});
        return booking;
    }

    /**
     * Create test payment data.
     */
    public static Map<String, Object> createTestPayment(String amount, String currency) {
        Map<String, Object> payment = new HashMap<>();

        Map<String, String> amountDto = new HashMap<>();
        amountDto.put("amount", amount);
        amountDto.put("currency", currency);
        payment.put("amount", amountDto);

        payment.put("paymentMethod", "CREDIT_CARD");

        return payment;
    }

    /**
     * Create malicious guest with injection payloads.
     */
    public static Map<String, Object> createMaliciousGuest(String injectionPayload) {
        return createTestGuest(injectionPayload, injectionPayload);
    }

    /**
     * Generate random UUID.
     */
    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    /**
     * Create CSRF token for testing.
     */
    public static String createCsrfToken() {
        return "test-csrf-token-" + UUID.randomUUID();
    }

    /**
     * Add CSRF token to request.
     */
    public static MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, String token) {
        return request.header("X-CSRF-TOKEN", token);
    }

    /**
     * Create SQL injection test data.
     */
    public static Map<String, String> createSqlInjectionTestData() {
        Map<String, String> data = new HashMap<>();
        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            data.put("sql_" + Math.abs(payload.hashCode()), payload);
        }
        return data;
    }

    /**
     * Create XSS test data.
     */
    public static Map<String, String> createXssTestData() {
        Map<String, String> data = new HashMap<>();
        for (String payload : InjectionPayloadProvider.getXssPayloads()) {
            data.put("xss_" + Math.abs(payload.hashCode()), payload);
        }
        return data;
    }

    /**
     * Validate that response does not contain sensitive data.
     */
    public static boolean containsSensitiveData(String response) {
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("password") ||
               lowerResponse.contains("secret") ||
               lowerResponse.contains("token") ||
               lowerResponse.contains("api_key") ||
               lowerResponse.contains("private_key");
    }

    /**
     * Create test user credentials.
     */
    public static Map<String, String> createTestCredentials(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        return credentials;
    }

    /**
     * Create weak password examples for testing.
     */
    public static String[] getWeakPasswords() {
        return new String[]{
            "123456",
            "password",
            "12345678",
            "qwerty",
            "abc123",
            "monkey",
            "letmein",
            "trustno1",
            "dragon",
            "baseball"
        };
    }

    /**
     * Create strong password for testing.
     */
    public static String getStrongPassword() {
        return "SecureP@ssw0rd!2024";
    }
}
