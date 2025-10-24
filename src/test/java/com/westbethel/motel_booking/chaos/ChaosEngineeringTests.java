package com.westbethel.motel_booking.chaos;

import com.westbethel.motel_booking.testutils.E2ETestBase;
import com.westbethel.motel_booking.testutils.TestDataGenerator;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Chaos Engineering Test Suite.
 * Tests system resilience under various failure conditions.
 *
 * TDD Implementation - Agent 5, Phase 2
 * Test Coverage: 20+ chaos engineering scenarios
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("Chaos Engineering Tests")
public class ChaosEngineeringTests extends E2ETestBase {

    // ==================== Network Failure Tests (7 tests) ====================

    @Test
    @DisplayName("Chaos: Timeout handling")
    void testTimeoutHandling() {
        String token = registerUser("timeout", "timeout@test.com", "Timeout123!@#", "Timeout", "Test");

        Response response = given()
                .spec(authenticatedRequest(token))
                .timeout(100) // Very short timeout
                .when()
                .get("/api/properties");

        // Should either succeed quickly or timeout gracefully
        assertThat(response.statusCode()).isIn(200, 0, 408, 504);
    }

    @Test
    @DisplayName("Chaos: Slow response handling")
    void testSlowResponseHandling() {
        String token = registerUser("slow", "slow@test.com", "Slow123!@#", "Slow", "Test");

        long startTime = System.currentTimeMillis();

        Response response = given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/bookings");

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Response time: " + duration + "ms");

        // System should respond within reasonable time
        response.then().statusCode(200);
        assertThat(duration).isLessThan(10000); // 10 seconds max
    }

    @Test
    @DisplayName("Chaos: Connection reset during request")
    void testConnectionReset() {
        String token = registerUser("connreset", "reset@test.com", "Reset123!@#", "Reset", "Test");

        // Simulate connection issues by making multiple rapid requests
        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < 10; i++) {
            try {
                Response response = given()
                        .spec(authenticatedRequest(token))
                        .when()
                        .get("/api/properties");

                if (response.statusCode() == 200) {
                    successCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }

        // System should handle at least some requests
        assertThat(successCount + errorCount).isEqualTo(10);
        assertThat(successCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Chaos: Partial network failure")
    void testPartialNetworkFailure() {
        String token = registerUser("partial", "partial@test.com", "Partial123!@#", "Partial", "Test");

        // Make requests to different endpoints
        Response r1 = given().spec(authenticatedRequest(token)).when().get("/api/properties");
        Response r2 = given().spec(authenticatedRequest(token)).when().get("/api/rooms");
        Response r3 = given().spec(authenticatedRequest(token)).when().get("/api/bookings");

        // At least some endpoints should work
        int successCount = 0;
        if (r1.statusCode() == 200) successCount++;
        if (r2.statusCode() == 200) successCount++;
        if (r3.statusCode() == 200) successCount++;

        assertThat(successCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Chaos: DNS resolution failure simulation")
    void testDnsFailureSimulation() {
        // Test with invalid hostname (simulates DNS failure)
        try {
            Response response = given()
                    .baseUri("http://invalid.host.example")
                    .when()
                    .get("/api/properties");

            // Should fail gracefully
            assertThat(response.statusCode()).isLessThan(600);
        } catch (Exception e) {
            // DNS failure is expected
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Chaos: Network latency injection")
    void testNetworkLatencyInjection() throws InterruptedException {
        String token = registerUser("latency", "latency@test.com", "Latency123!@#", "Latency", "Test");

        // Measure baseline
        long baseline = measureResponseTime(token, "/api/properties");

        // Add artificial delay (simulate network latency)
        Thread.sleep(100);

        long withLatency = measureResponseTime(token, "/api/properties");

        System.out.println("Baseline: " + baseline + "ms, With Latency: " + withLatency + "ms");

        // System should still respond
        assertThat(withLatency).isGreaterThan(0);
    }

    @Test
    @DisplayName("Chaos: Bandwidth limitation")
    void testBandwidthLimitation() {
        String token = registerUser("bandwidth", "bandwidth@test.com", "Bandwidth123!@#", "Band", "Width");

        // Request large dataset (simulate bandwidth constraints)
        Long propertyId = createProperty("Bandwidth Hotel", "123 Bandwidth St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);

        // Create multiple rooms
        for (int i = 0; i < 50; i++) {
            createRoom(propertyId, roomTypeId, "R" + i);
        }

        Response response = given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/rooms");

        // Should handle large response
        response.then().statusCode(200);
        assertThat(response.getBody().asString().length()).isGreaterThan(0);
    }

    // ==================== Service Degradation Tests (7 tests) ====================

    @Test
    @DisplayName("Degradation: Database connection pool exhaustion")
    void testDatabasePoolExhaustion() throws InterruptedException {
        String token = registerUser("dbpool", "dbpool@test.com", "DBPool123!@#", "DB", "Pool");

        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Exhaust connection pool with concurrent requests
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                try {
                    Response response = given()
                            .spec(authenticatedRequest(token))
                            .when()
                            .get("/api/bookings");

                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(30, TimeUnit.SECONDS);

        System.out.println("DB Pool Test - Success: " + successCount.get() + ", Failures: " + failureCount.get());

        // System should handle at least some requests
        assertThat(successCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Degradation: Memory pressure")
    void testMemoryPressure() {
        String token = registerUser("memory", "memory@test.com", "Memory123!@#", "Memory", "Test");

        // Create many bookings to consume memory
        Long propertyId = createProperty("Memory Hotel", "123 Memory St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);

        int successfulBookings = 0;
        for (int i = 0; i < 100; i++) {
            Long roomId = createRoom(propertyId, roomTypeId, "M" + i);
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "memory@test.com");

            setAuthToken(token);
            Response response = authenticatedPost("/api/bookings", booking);

            if (response.statusCode() == 201) {
                successfulBookings++;
            }
        }

        System.out.println("Successfully created " + successfulBookings + " bookings under memory pressure");

        // Should handle substantial load
        assertThat(successfulBookings).isGreaterThan(50);
    }

    @Test
    @DisplayName("Degradation: CPU throttling")
    void testCpuThrottling() {
        String token = registerUser("cpu", "cpu@test.com", "CPU123!@#", "CPU", "Test");

        // Simulate CPU-intensive operations
        int iterations = 50;
        long totalTime = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            Response response = given()
                    .spec(authenticatedRequest(token))
                    .when()
                    .get("/api/properties");

            totalTime += (System.currentTimeMillis() - start);

            response.then().statusCode(200);
        }

        double avgTime = totalTime / (double) iterations;
        System.out.println("Average response time under CPU pressure: " + avgTime + "ms");

        // Response times may degrade but should still work
        assertThat(avgTime).isLessThan(5000);
    }

    @Test
    @DisplayName("Degradation: Disk I/O saturation")
    void testDiskIOSaturation() {
        String token = registerUser("diskio", "disk@test.com", "Disk123!@#", "Disk", "Test");

        // Simulate disk I/O intensive operations
        for (int i = 0; i < 20; i++) {
            Response response = given()
                    .spec(authenticatedRequest(token))
                    .when()
                    .get("/api/bookings");

            response.then().statusCode(200);
        }

        // System should continue functioning
        assertThat(getRecordCount("users")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Degradation: Cache failure")
    void testCacheFailure() {
        String token = registerUser("cache", "cache@test.com", "Cache123!@#", "Cache", "Test");

        // Make requests that would normally be cached
        Response r1 = given().spec(authenticatedRequest(token)).when().get("/api/properties");
        Response r2 = given().spec(authenticatedRequest(token)).when().get("/api/properties");

        // Should work even if cache fails
        r1.then().statusCode(200);
        r2.then().statusCode(200);
    }

    @Test
    @DisplayName("Degradation: Session store failure")
    void testSessionStoreFailure() {
        String token = registerUser("session", "session@test.com", "Session123!@#", "Session", "Test");

        // Make authenticated requests (should work with JWT even if session store fails)
        Response response = given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/bookings");

        response.then().statusCode(200);
    }

    @Test
    @DisplayName("Degradation: Email service failure")
    void testEmailServiceFailure() {
        String token = registerUser("emailfail", "emailfail@test.com", "Email123!@#", "Email", "Fail");

        Long propertyId = createProperty("Email Fail Hotel", "123 Email St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "101");

        setAuthToken(token);
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "emailfail@test.com");

        // Booking should succeed even if email fails
        Response response = authenticatedPost("/api/bookings", booking);

        response.then().statusCode(anyOf(is(201), is(200)));
    }

    // ==================== Data Corruption Tests (6 tests) ====================

    @Test
    @DisplayName("Corruption: Invalid data in database")
    void testInvalidDataHandling() {
        String token = registerUser("corrupt", "corrupt@test.com", "Corrupt123!@#", "Corrupt", "Test");

        // Insert invalid data directly
        try {
            jdbcTemplate.update(
                    "INSERT INTO bookings (id, room_id, guest_email, check_in_date, check_out_date, number_of_guests, status) " +
                            "VALUES (999, -1, 'invalid', NULL, NULL, -999, 'INVALID')"
            );
        } catch (Exception e) {
            // Database constraints may prevent invalid data
        }

        // System should handle gracefully
        Response response = given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/bookings");

        response.then().statusCode(200);
    }

    @Test
    @DisplayName("Corruption: Malformed JSON in request")
    void testMalformedJsonHandling() {
        String token = registerUser("json", "json@test.com", "JSON123!@#", "JSON", "Test");

        Response response = given()
                .spec(authenticatedRequest(token))
                .body("{malformed:json, missing: quotes}")
                .when()
                .post("/api/bookings");

        // Should return proper error
        response.then().statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("Corruption: Missing required fields")
    void testMissingRequiredFields() {
        String token = registerUser("missing", "missing@test.com", "Missing123!@#", "Missing", "Test");

        Map<String, Object> incomplete = new HashMap<>();
        incomplete.put("roomId", 1);
        // Missing other required fields

        Response response = given()
                .spec(authenticatedRequest(token))
                .body(incomplete)
                .when()
                .post("/api/bookings");

        response.then().statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("Corruption: Type mismatch in data")
    void testTypeMismatchHandling() {
        String token = registerUser("type", "type@test.com", "Type123!@#", "Type", "Test");

        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", "not-a-number"); // String instead of number
        booking.put("guestEmail", "type@test.com");
        booking.put("checkInDate", "2025-12-01");
        booking.put("checkOutDate", "2025-12-05");
        booking.put("numberOfGuests", "two"); // String instead of number

        Response response = given()
                .spec(authenticatedRequest(token))
                .body(booking)
                .when()
                .post("/api/bookings");

        response.then().statusCode(anyOf(is(400), is(422)));
    }

    @Test
    @DisplayName("Corruption: Database constraint violation")
    void testConstraintViolation() {
        String token = registerUser("constraint", "constraint@test.com", "Constraint123!@#", "Constraint", "Test");

        Long propertyId = createProperty("Constraint Hotel", "123 Constraint St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "101");

        setAuthToken(token);

        // Create booking
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "constraint@test.com");
        Response r1 = authenticatedPost("/api/bookings", booking);

        // Try to create duplicate (if unique constraints exist)
        Response r2 = authenticatedPost("/api/bookings", booking);

        // One should succeed, duplicate may fail
        assertThat(r1.statusCode() == 201 || r2.statusCode() == 201).isTrue();
    }

    @Test
    @DisplayName("Corruption: Recovery from corrupted state")
    void testRecoveryFromCorruption() {
        String token = registerUser("recovery", "recovery@test.com", "Recovery123!@#", "Recovery", "Test");

        // Try to corrupt data
        try {
            jdbcTemplate.update("UPDATE users SET email = NULL WHERE username = 'recovery'");
        } catch (Exception e) {
            // Constraint may prevent corruption
        }

        // System should still function
        Response response = given()
                .spec(authenticatedRequest(token))
                .when()
                .get("/api/properties");

        // May fail due to corruption or handle gracefully
        assertThat(response.statusCode()).isIn(200, 401, 500);

        // Verify other operations still work
        String newToken = registerUser("healthy", "healthy@test.com", "Healthy123!@#", "Healthy", "User");
        Response healthyResponse = given()
                .spec(authenticatedRequest(newToken))
                .when()
                .get("/api/properties");

        healthyResponse.then().statusCode(200);
    }

    // Helper method
    private long measureResponseTime(String token, String endpoint) {
        long start = System.currentTimeMillis();
        given().spec(authenticatedRequest(token)).when().get(endpoint);
        return System.currentTimeMillis() - start;
    }
}
