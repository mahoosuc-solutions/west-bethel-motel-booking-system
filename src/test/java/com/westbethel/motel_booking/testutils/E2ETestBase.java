package com.westbethel.motel_booking.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/**
 * Enhanced base class for comprehensive E2E integration tests.
 * Provides REST Assured configuration, authentication helpers, and test utilities.
 *
 * TDD Implementation for Agent 5 - Phase 2
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.mail.host=localhost",
                "spring.mail.port=3025",
                "spring.cache.type=none"
        }
)
@ActiveProfiles("test")
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected RequestSpecification requestSpec;
    protected String authToken;
    protected String adminToken;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(port)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();

        cleanupDatabase();
        authToken = null;
        adminToken = null;
    }

    /**
     * Clean up all test data before each test.
     * Order matters due to foreign key constraints.
     */
    protected void cleanupDatabase() {
        try {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // Clean up in reverse dependency order
            jdbcTemplate.execute("TRUNCATE TABLE payments CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE invoice_line_items CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE invoices CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE bookings CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE rooms CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE rate_plans CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE room_types CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE guests CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE loyalty_profiles CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE properties CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE email_queue CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE token_blacklist CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE audit_log CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE security_events CASCADE");

            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception e) {
            // Ignore errors in cleanup
        }
    }

    // ==================== Authentication Methods ====================

    /**
     * Register a new user and return the authentication token.
     */
    protected String registerUser(String username, String email, String password, String firstName, String lastName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("firstName", firstName);
        requestBody.put("lastName", lastName);

        return given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .path("token");
    }

    /**
     * Register a new admin user and return the authentication token.
     */
    protected String registerAdmin(String username, String email, String password) {
        String token = registerUser(username, email, password, "Admin", "User");

        // Promote user to admin role
        jdbcTemplate.update(
                "UPDATE users SET role = 'ADMIN' WHERE username = ?",
                username
        );

        return token;
    }

    /**
     * Login an existing user and return the JWT token.
     */
    protected String loginUser(String username, String password) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        return given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    /**
     * Get authenticated request specification with Bearer token.
     */
    protected RequestSpecification authenticatedRequest() {
        if (authToken == null) {
            throw new IllegalStateException("No auth token available. Call loginUser() or registerUser() first.");
        }
        return authenticatedRequest(authToken);
    }

    /**
     * Get authenticated request specification with custom token.
     */
    protected RequestSpecification authenticatedRequest(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Get admin authenticated request specification.
     */
    protected RequestSpecification adminRequest() {
        if (adminToken == null) {
            adminToken = registerAdmin("admin", "admin@test.com", "Admin123!@#");
        }
        return authenticatedRequest(adminToken);
    }

    // ==================== Data Creation Helpers ====================

    /**
     * Create a test property and return its ID.
     */
    protected Long createProperty(String name, String address) {
        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("address", address);
        property.put("city", "Test City");
        property.put("state", "TS");
        property.put("country", "USA");
        property.put("zipCode", "12345");
        property.put("phone", "555-0100");
        property.put("email", "property@test.com");

        return given()
                .spec(adminRequest())
                .body(property)
                .when()
                .post("/api/properties")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Create a test room type and return its ID.
     */
    protected Long createRoomType(Long propertyId, String name, String code, int maxOccupancy) {
        Map<String, Object> roomType = new HashMap<>();
        roomType.put("propertyId", propertyId);
        roomType.put("name", name);
        roomType.put("code", code);
        roomType.put("description", "Test " + name);
        roomType.put("maxOccupancy", maxOccupancy);
        roomType.put("baseRate", 100.00);

        return given()
                .spec(adminRequest())
                .body(roomType)
                .when()
                .post("/api/room-types")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Create a test room and return its ID.
     */
    protected Long createRoom(Long propertyId, Long roomTypeId, String roomNumber) {
        Map<String, Object> room = new HashMap<>();
        room.put("propertyId", propertyId);
        room.put("roomTypeId", roomTypeId);
        room.put("roomNumber", roomNumber);
        room.put("floor", 1);
        room.put("status", "AVAILABLE");

        return given()
                .spec(adminRequest())
                .body(room)
                .when()
                .post("/api/rooms")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Create a test booking and return its ID.
     */
    protected Long createBooking(Long roomId, String guestEmail, LocalDate checkIn, LocalDate checkOut) {
        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", roomId);
        booking.put("guestEmail", guestEmail);
        booking.put("checkInDate", checkIn.format(DateTimeFormatter.ISO_DATE));
        booking.put("checkOutDate", checkOut.format(DateTimeFormatter.ISO_DATE));
        booking.put("numberOfGuests", 2);
        booking.put("specialRequests", "Test booking");

        return given()
                .spec(authenticatedRequest())
                .body(booking)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Create a test guest and return the guest object.
     */
    protected Map<String, Object> createGuest(String email, String firstName, String lastName) {
        Map<String, Object> guest = new HashMap<>();
        guest.put("email", email);
        guest.put("firstName", firstName);
        guest.put("lastName", lastName);
        guest.put("phone", "555-0100");

        Response response = given()
                .spec(authenticatedRequest())
                .body(guest)
                .when()
                .post("/api/guests")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.as(Map.class);
    }

    // ==================== Verification Helpers ====================

    /**
     * Verify email for a user (simulates clicking verification link).
     */
    protected void verifyUserEmail(String username) {
        jdbcTemplate.update(
                "UPDATE users SET email_verified = true WHERE username = ?",
                username
        );
    }

    /**
     * Wait for asynchronous operations to complete.
     */
    protected void waitForAsyncOperations() {
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> true);
    }

    /**
     * Wait for a condition to be true.
     */
    protected void waitUntil(java.util.function.BooleanSupplier condition, int timeoutSeconds) {
        await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(condition::getAsBoolean);
    }

    /**
     * Get count of records in a table.
     */
    protected int getRecordCount(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Integer.class
        );
        return count != null ? count : 0;
    }

    /**
     * Check if a record exists by ID.
     */
    protected boolean recordExists(String tableName, String idColumn, Object id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    /**
     * Get a single field value from database.
     */
    protected <T> T getFieldValue(String tableName, String fieldName, String idColumn, Object id, Class<T> type) {
        return jdbcTemplate.queryForObject(
                "SELECT " + fieldName + " FROM " + tableName + " WHERE " + idColumn + " = ?",
                type,
                id
        );
    }

    /**
     * Set authentication token for subsequent requests.
     */
    protected void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Get base URL for the application.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Perform a GET request and return the response.
     */
    protected Response get(String path) {
        return given()
                .spec(requestSpec)
                .when()
                .get(path);
    }

    /**
     * Perform an authenticated GET request and return the response.
     */
    protected Response authenticatedGet(String path) {
        return given()
                .spec(authenticatedRequest())
                .when()
                .get(path);
    }

    /**
     * Perform an authenticated POST request and return the response.
     */
    protected Response authenticatedPost(String path, Object body) {
        return given()
                .spec(authenticatedRequest())
                .body(body)
                .when()
                .post(path);
    }

    /**
     * Perform an authenticated PUT request and return the response.
     */
    protected Response authenticatedPut(String path, Object body) {
        return given()
                .spec(authenticatedRequest())
                .body(body)
                .when()
                .put(path);
    }

    /**
     * Perform an authenticated DELETE request and return the response.
     */
    protected Response authenticatedDelete(String path) {
        return given()
                .spec(authenticatedRequest())
                .when()
                .delete(path);
    }
}
