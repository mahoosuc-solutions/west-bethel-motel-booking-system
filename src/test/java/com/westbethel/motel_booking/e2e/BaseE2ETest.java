package com.westbethel.motel_booking.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

/**
 * Base class for End-to-End integration tests.
 * Provides REST Assured configuration and common test utilities.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@Import(E2ETestConfiguration.class)
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected RequestSpecification requestSpec;
    protected String authToken;

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
    }

    /**
     * Clean up all test data before each test.
     * Order matters due to foreign key constraints.
     */
    protected void cleanupDatabase() {
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

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    /**
     * Register a new user and return the authentication response.
     */
    protected String registerUser(String username, String email, String password, String firstName, String lastName) {
        String requestBody = String.format(
                "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\"}",
                username, email, password, firstName, lastName
        );

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
     * Login an existing user and return the JWT token.
     */
    protected String loginUser(String username, String password) {
        String requestBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password
        );

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

        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();
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
     * Verify email for a user (simulates clicking verification link).
     */
    protected void verifyUserEmail(String username) {
        // In a real scenario, we'd extract the verification token from the email
        // For testing, we'll directly update the database
        jdbcTemplate.update(
                "UPDATE users SET email_verified = true WHERE username = ?",
                username
        );
    }

    /**
     * Set the authentication token for subsequent requests.
     */
    protected void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Wait for asynchronous operations to complete.
     */
    protected void waitForAsyncOperations() throws InterruptedException {
        Thread.sleep(1000); // Wait 1 second for async operations
    }

    /**
     * Get count of records in a table.
     */
    protected int getRecordCount(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Integer.class
        );
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
}
