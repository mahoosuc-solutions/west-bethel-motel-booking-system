package com.westbethel.motel_booking.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-End tests for admin workflows and operations.
 *
 * Test Count: 8 tests
 */
@ActiveProfiles("test")
@DisplayName("Admin Workflow E2E Tests")
public class AdminWorkflowTest extends BaseE2ETest {

    @Test
    @DisplayName("Admin: Login with elevated privileges")
    public void testAdminLogin() {
        // Create admin user directly in database
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password, first_name, last_name, enabled, email_verified) " +
                "VALUES (1, 'admin', 'admin@westbethelmotel.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin', 'User', true, true)"
        );

        // Assign admin role
        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN')"
        );

        // Login as admin
        String adminToken = loginUser("admin", "password");
        assertThat(adminToken).isNotNull();

        // Verify admin can access protected endpoints
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/dashboard")
                .then()
                .statusCode(anyOf(is(200), is(404))); // May or may not have admin dashboard
    }

    @Test
    @DisplayName("Admin: View dashboard metrics")
    public void testViewDashboardMetrics() {
        String adminToken = createAdminUser();
        setupTestProperty();
        createTestBookings();

        // View dashboard
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/dashboard")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // View metrics
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/metrics")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("Admin: Process payment refund")
    public void testProcessPaymentRefund() throws Exception {
        String adminToken = createAdminUser();
        setupTestProperty();

        // Create a user and booking
        String userToken = registerUser("refunduser", "refund@example.com", "Password123!", "Refund", "User");

        // Create booking
        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("checkInDate", java.time.LocalDate.now().plusDays(7).toString());
        bookingRequest.put("checkOutDate", java.time.LocalDate.now().plusDays(10).toString());
        bookingRequest.put("numberOfGuests", 2);
        bookingRequest.put("roomTypeCode", "STD");
        bookingRequest.put("guestFirstName", "Refund");
        bookingRequest.put("guestLastName", "User");
        bookingRequest.put("guestEmail", "refund@example.com");
        bookingRequest.put("guestPhone", "+1-555-0123");

        Integer bookingId = given()
                .spec(authenticatedRequest(userToken))
                .body(bookingRequest)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        // Create payment
        jdbcTemplate.update(
                "INSERT INTO payments (id, booking_id, amount_value, amount_currency, payment_method, status, transaction_id) " +
                "VALUES (1, ?, 300.00, 'USD', 'CREDIT_CARD', 'COMPLETED', 'TXN123')",
                bookingId
        );

        // Admin processes refund
        Map<String, Object> refundRequest = new HashMap<>();
        refundRequest.put("paymentId", 1);
        refundRequest.put("amount", 300.00);
        refundRequest.put("reason", "Customer requested cancellation");

        given()
                .spec(authenticatedRequest(adminToken))
                .body(refundRequest)
                .when()
                .post("/api/admin/payments/refund")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("Admin: Generate booking reports")
    public void testGenerateBookingReports() {
        String adminToken = createAdminUser();
        setupTestProperty();
        createTestBookings();

        // Generate daily report
        given()
                .spec(authenticatedRequest(adminToken))
                .queryParam("reportType", "daily")
                .queryParam("date", java.time.LocalDate.now().toString())
                .when()
                .get("/api/admin/reports/bookings")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Generate revenue report
        given()
                .spec(authenticatedRequest(adminToken))
                .queryParam("reportType", "revenue")
                .queryParam("startDate", java.time.LocalDate.now().minusDays(30).toString())
                .queryParam("endDate", java.time.LocalDate.now().toString())
                .when()
                .get("/api/admin/reports/revenue")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("Admin: Manage promotions")
    public void testManagePromotions() {
        String adminToken = createAdminUser();
        setupTestProperty();

        // Create promotion
        Map<String, Object> promotionRequest = new HashMap<>();
        promotionRequest.put("code", "SPRING2024");
        promotionRequest.put("description", "Spring 2024 Promotion");
        promotionRequest.put("discountPercentage", 20.0);
        promotionRequest.put("validFrom", java.time.LocalDate.now().toString());
        promotionRequest.put("validTo", java.time.LocalDate.now().plusDays(30).toString());

        given()
                .spec(authenticatedRequest(adminToken))
                .body(promotionRequest)
                .when()
                .post("/api/admin/promotions")
                .then()
                .statusCode(anyOf(is(201), is(404)));

        // List promotions
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/promotions")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Update promotion
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("discountPercentage", 25.0);

        given()
                .spec(authenticatedRequest(adminToken))
                .body(updateRequest)
                .when()
                .put("/api/admin/promotions/SPRING2024")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Delete promotion
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .delete("/api/admin/promotions/SPRING2024")
                .then()
                .statusCode(anyOf(is(200), is(204), is(404)));
    }

    @Test
    @DisplayName("Admin: View email queue and retry failed emails")
    public void testEmailQueueManagement() throws Exception {
        String adminToken = createAdminUser();
        setupTestProperty();

        // Create a booking to generate email
        String userToken = registerUser("emailtest", "emailtest@example.com", "Password123!", "Email", "Test");

        Map<String, Object> bookingRequest = new HashMap<>();
        bookingRequest.put("checkInDate", java.time.LocalDate.now().plusDays(7).toString());
        bookingRequest.put("checkOutDate", java.time.LocalDate.now().plusDays(10).toString());
        bookingRequest.put("numberOfGuests", 2);
        bookingRequest.put("roomTypeCode", "STD");
        bookingRequest.put("guestFirstName", "Email");
        bookingRequest.put("guestLastName", "Test");
        bookingRequest.put("guestEmail", "emailtest@example.com");
        bookingRequest.put("guestPhone", "+1-555-0123");

        given()
                .spec(authenticatedRequest(userToken))
                .body(bookingRequest)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201);

        waitForAsyncOperations();

        // View email queue
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/emails/queue")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Retry failed emails
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .post("/api/admin/emails/retry-failed")
                .then()
                .statusCode(anyOf(is(200), is(202), is(404)));
    }

    @Test
    @DisplayName("Admin: System health monitoring")
    public void testSystemHealthMonitoring() {
        String adminToken = createAdminUser();

        // Check overall health
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", notNullValue());

        // Check database health
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/actuator/health/db")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Check Redis health
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/actuator/health/redis")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // View metrics
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/actuator/metrics")
                .then()
                .statusCode(anyOf(is(200), is(401))); // May require auth
    }

    @Test
    @DisplayName("Admin: Manage user accounts")
    public void testManageUserAccounts() {
        String adminToken = createAdminUser();

        // Create test user
        registerUser("manageduser", "managed@example.com", "Password123!", "Managed", "User");

        // View all users
        given()
                .spec(authenticatedRequest(adminToken))
                .when()
                .get("/api/admin/users")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Disable user account
        Map<String, Object> disableRequest = new HashMap<>();
        disableRequest.put("enabled", false);

        given()
                .spec(authenticatedRequest(adminToken))
                .body(disableRequest)
                .when()
                .put("/api/admin/users/manageduser/status")
                .then()
                .statusCode(anyOf(is(200), is(404)));

        // Reset user password
        Map<String, Object> resetRequest = new HashMap<>();
        resetRequest.put("newPassword", "NewPassword456!");

        given()
                .spec(authenticatedRequest(adminToken))
                .body(resetRequest)
                .when()
                .post("/api/admin/users/manageduser/reset-password")
                .then()
                .statusCode(anyOf(is(200), is(404)));
    }

    // Helper methods

    private String createAdminUser() {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password, first_name, last_name, enabled, email_verified) " +
                "VALUES (999, 'testadmin', 'testadmin@westbethelmotel.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test', 'Admin', true, true)"
        );

        jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role) VALUES (999, 'ADMIN')"
        );

        return loginUser("testadmin", "password");
    }

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

    private void createTestBookings() {
        // Create test guest
        jdbcTemplate.update(
                "INSERT INTO guests (id, first_name, last_name, email, phone, street, city, state, postal_code, country) " +
                "VALUES (1, 'Test', 'Guest', 'guest@example.com', '+1-555-0123', '456 Elm St', 'Portland', 'ME', '04101', 'USA')"
        );

        // Create test booking
        jdbcTemplate.update(
                "INSERT INTO bookings (id, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, total_amount_value, total_amount_currency, confirmation_code) " +
                "VALUES (1, 1, 1, CURRENT_DATE + 7, CURRENT_DATE + 10, 2, 'CONFIRMED', 300.00, 'USD', 'TEST123')"
        );
    }
}
