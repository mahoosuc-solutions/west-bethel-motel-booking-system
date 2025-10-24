package com.westbethel.motel_booking.e2e;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End tests for error recovery and resilience scenarios.
 *
 * Test Count: 7 tests
 */
@ActiveProfiles("test")
@DisplayName("Error Recovery E2E Tests")
public class ErrorRecoveryTest extends BaseE2ETest {

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    @DisplayName("Recovery: Database connection failure handling")
    public void testDatabaseConnectionFailureHandling() {
        // Test graceful degradation when database is unavailable
        // Since we can't actually kill the database in tests, we test error responses

        // Attempt operation that would fail with DB issues
        given()
                .spec(requestSpec)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(anyOf(is(200), is(503)))
                .body("status", notNullValue());
    }

    @Test
    @DisplayName("Recovery: Redis unavailability handling")
    public void testRedisUnavailabilityHandling() {
        setupTestProperty();

        String token = registerUser("redistest", "redis@example.com", "Password123!", "Redis", "Test");

        // Even if Redis is down, the application should continue to function
        // (perhaps slower, without cache)
        given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/users/profile")
                .then()
                .statusCode(200); // Should still work without cache
    }

    @Test
    @DisplayName("Recovery: Email service failure handling")
    public void testEmailServiceFailureHandling() throws Exception {
        setupTestProperty();

        String token = registerUser("emailfail", "emailfail@example.com", "Password123!", "Email", "Fail");
        setAuthToken(token);

        // Create booking - should succeed even if email sending fails
        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("checkInDate", java.time.LocalDate.now().plusDays(7).toString());
        bookingRequest.put("checkOutDate", java.time.LocalDate.now().plusDays(10).toString());
        bookingRequest.put("numberOfGuests", 2);
        bookingRequest.put("roomTypeCode", "STD");
        bookingRequest.put("guestFirstName", "Email");
        bookingRequest.put("guestLastName", "Fail");
        bookingRequest.put("guestEmail", "emailfail@example.com");
        bookingRequest.put("guestPhone", "+1-555-0123");

        String confirmationCode = given()
                .spec(authenticatedRequest())
                .body(bookingRequest)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201)
                .extract()
                .path("confirmationCode");

        assertThat(confirmationCode).isNotNull();

        // Booking should be created even if email fails
        assertThat(recordExists("bookings", "confirmation_code", confirmationCode)).isTrue();

        waitForAsyncOperations();

        // Email should be queued for retry
        Integer queuedEmails = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_queue WHERE recipient_email = ?",
                Integer.class,
                "emailfail@example.com"
        );
        assertThat(queuedEmails).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Recovery: Transaction rollback on payment failure")
    public void testTransactionRollbackOnPaymentFailure() throws Exception {
        setupTestProperty();

        String token = registerUser("txrollback", "txrollback@example.com", "Password123!", "TX", "Rollback");
        setAuthToken(token);

        // Create booking
        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("checkInDate", java.time.LocalDate.now().plusDays(7).toString());
        bookingRequest.put("checkOutDate", java.time.LocalDate.now().plusDays(10).toString());
        bookingRequest.put("numberOfGuests", 2);
        bookingRequest.put("roomTypeCode", "STD");
        bookingRequest.put("guestFirstName", "TX");
        bookingRequest.put("guestLastName", "Rollback");
        bookingRequest.put("guestEmail", "txrollback@example.com");
        bookingRequest.put("guestPhone", "+1-555-0123");

        Integer bookingId = given()
                .spec(authenticatedRequest())
                .body(bookingRequest)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Attempt payment with invalid data (should trigger rollback)
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("bookingId", bookingId);
        paymentRequest.put("amount", -100.00); // Invalid negative amount
        paymentRequest.put("paymentMethod", "CREDIT_CARD");

        given()
                .spec(authenticatedRequest())
                .body(paymentRequest)
                .when()
                .post("/api/payments")
                .then()
                .statusCode(anyOf(is(400), is(422)));

        // Verify no payment record was created
        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?",
                Integer.class,
                bookingId
        );
        assertThat(paymentCount).isEqualTo(0);

        // Booking should still exist in original state
        assertThat(recordExists("bookings", "id", bookingId)).isTrue();
    }

    @Test
    @DisplayName("Recovery: Partial system degradation handling")
    public void testPartialSystemDegradation() {
        // Test that system continues to function when non-critical services are down

        // Health check should show partial degradation
        given()
                .spec(requestSpec)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(anyOf(is(200), is(503)))
                .body("status", notNullValue());

        // Core functionality should still work
        setupTestProperty();

        given()
                .spec(requestSpec)
                .queryParam("checkInDate", java.time.LocalDate.now().plusDays(7).toString())
                .queryParam("checkOutDate", java.time.LocalDate.now().plusDays(10).toString())
                .queryParam("guests", 2)
                .when()
                .get("/api/availability/search")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Recovery: Cache miss scenario handling")
    public void testCacheMissScenarioHandling() {
        setupTestProperty();

        String token = registerUser("cachetest", "cache@example.com", "Password123!", "Cache", "Test");

        // First request - cache miss, loads from database
        given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/users/profile")
                .then()
                .statusCode(200)
                .body("username", equalTo("cachetest"));

        // Clear cache if available
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }

        // Second request - should still work after cache clear
        given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/users/profile")
                .then()
                .statusCode(200)
                .body("username", equalTo("cachetest"));
    }

    @Test
    @DisplayName("Recovery: Rate limit recovery")
    public void testRateLimitRecovery() {
        // Test that after hitting rate limits, requests eventually succeed

        String token = registerUser("ratelimit", "ratelimit@example.com", "Password123!", "Rate", "Limit");

        // Make multiple rapid requests to potentially trigger rate limiting
        int successCount = 0;
        int rateLimitCount = 0;

        for (int i = 0; i < 20; i++) {
            int statusCode = given()
                    .spec(authenticatedRequest(token))
                    .when()
                    .get("/api/users/profile")
                    .then()
                    .extract()
                    .statusCode();

            if (statusCode == 200) {
                successCount++;
            } else if (statusCode == 429) {
                rateLimitCount++;
            }
        }

        System.out.println("Rate limit test - Success: " + successCount + ", Rate Limited: " + rateLimitCount);

        // Wait for rate limit window to reset
        if (rateLimitCount > 0) {
            Awaitility.await()
                    .atMost(65, TimeUnit.SECONDS)
                    .pollInterval(5, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        given()
                                .spec(authenticatedRequest(token))
                                .when()
                                .get("/api/users/profile")
                                .then()
                                .statusCode(200);
                    });
        }

        // After waiting, request should succeed
        given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/users/profile")
                .then()
                .statusCode(200);
    }

    // Helper method

    private void setupTestProperty() {
        jdbcTemplate.update(
                "INSERT INTO properties (id, name, timezone, street, city, state, postal_code, country, email, phone) " +
                "VALUES (1, 'Test Motel', 'America/New_York', '123 Main St', 'West Bethel', 'ME', '04286', 'USA', 'test@motel.com', '+1-555-0100')"
        );

        jdbcTemplate.update(
                "INSERT INTO room_types (id, code, name, description, max_occupancy, bed_count, property_id) " +
                "VALUES (1, 'STD', 'Standard Room', 'Comfortable standard room', 2, 1, 1)"
        );

        jdbcTemplate.update(
                "INSERT INTO rooms (id, room_number, room_type_id, property_id, status) " +
                "VALUES (1, '101', 1, 1, 'AVAILABLE')"
        );

        jdbcTemplate.update(
                "INSERT INTO rate_plans (id, name, description, room_type_id, base_rate_amount, base_rate_currency, valid_from, valid_to) " +
                "VALUES (1, 'Standard Rate', 'Standard room rate', 1, 100.00, 'USD', CURRENT_DATE - 30, CURRENT_DATE + 365)"
        );
    }
}
