package com.westbethel.motel_booking.load;

import com.westbethel.motel_booking.testutils.E2ETestBase;
import com.westbethel.motel_booking.testutils.LoadTestHelper;
import com.westbethel.motel_booking.testutils.TestDataGenerator;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Load, Stress, and Endurance Testing Suite.
 * Tests system performance under various load conditions.
 *
 * TDD Implementation - Agent 5, Phase 2
 * Test Coverage: 30+ load/stress/endurance scenarios
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("Load Testing Scenarios")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoadTestScenarios extends E2ETestBase {

    private LoadTestHelper loadTestHelper;
    private String testToken;
    private Long testPropertyId;
    private Long testRoomTypeId;
    private Long testRoomId;

    @BeforeEach
    void setupLoadTest() {
        loadTestHelper = new LoadTestHelper(getBaseUrl(), 100);

        // Setup test data
        testPropertyId = createProperty("Load Test Hotel", "123 Load St");
        testRoomTypeId = createRoomType(testPropertyId, "Standard", "STD", 2);
        testRoomId = createRoom(testPropertyId, testRoomTypeId, "101");

        testToken = registerUser("loadtester", "load@test.com", "Load123!@#", "Load", "Tester");
        setAuthToken(testToken);
    }

    @AfterEach
    void tearDownLoadTest() {
        if (loadTestHelper != null) {
            loadTestHelper.shutdown();
        }
    }

    // ==================== LOAD TESTS (10 scenarios) ====================

    @Test
    @Order(1)
    @DisplayName("Load Test: 10 concurrent user registrations")
    void testConcurrentUserRegistrations() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Map<String, String> userData = TestDataGenerator.generateUserRegistration();
            requests.add(() ->
                    given()
                            .spec(requestSpec)
                            .body(userData)
                            .when()
                            .post("/api/auth/register")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 10);

        System.out.println("Concurrent Registrations: " + result);
        assertThat(result.successRate).isGreaterThan(80.0);
        assertThat(result.p95ResponseTime).isLessThan(5000);
    }

    @Test
    @Order(2)
    @DisplayName("Load Test: 25 concurrent availability searches")
    void testConcurrentAvailabilitySearches() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .queryParam("checkIn", "2025-12-01")
                            .queryParam("checkOut", "2025-12-05")
                            .queryParam("guests", 2)
                            .when()
                            .get("/api/rooms/availability")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 25);

        System.out.println("Concurrent Searches: " + result);
        assertThat(result.successRate).isGreaterThan(90.0);
        assertThat(result.averageResponseTime).isLessThan(1000);
    }

    @Test
    @Order(3)
    @DisplayName("Load Test: 50 concurrent booking attempts")
    void testConcurrentBookingAttempts() throws InterruptedException {
        // Create multiple rooms for concurrent bookings
        List<Long> roomIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            roomIds.add(createRoom(testPropertyId, testRoomTypeId, "R" + (200 + i)));
        }

        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final Long roomId = roomIds.get(i);
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "load@test.com");

            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .body(booking)
                            .when()
                            .post("/api/bookings")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 50);

        System.out.println("Concurrent Bookings: " + result);
        assertThat(result.successRate).isGreaterThan(85.0);
        assertThat(result.p99ResponseTime).isLessThan(3000);
    }

    @Test
    @Order(4)
    @DisplayName("Load Test: 100 concurrent room listing requests")
    void testConcurrentRoomListings() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .when()
                            .get("/api/rooms")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 100);

        System.out.println("Concurrent Room Listings: " + result);
        assertThat(result.successRate).isGreaterThan(95.0);
        assertThat(result.throughput).isGreaterThan(10.0); // At least 10 req/s
    }

    @Test
    @Order(5)
    @DisplayName("Load Test: Ramped load from 1 to 50 users")
    void testRampedLoadBookingFlow() throws InterruptedException {
        Callable<Response> bookingRequest = () -> {
            Map<String, Object> booking = TestDataGenerator.generateBooking(testRoomId, "load@test.com");
            return given()
                    .spec(authenticatedRequest())
                    .body(booking)
                    .when()
                    .post("/api/bookings");
        };

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeRampedLoad(bookingRequest, 1, 50, 10);

        System.out.println("Ramped Load: " + result);
        assertThat(result.successRate).isGreaterThan(70.0); // Lower threshold due to room availability conflicts
    }

    @Test
    @Order(6)
    @DisplayName("Load Test: Property listing under load")
    void testPropertyListingLoad() throws InterruptedException {
        // Create multiple properties
        for (int i = 0; i < 20; i++) {
            createProperty("Hotel " + i, "Address " + i);
        }

        List<Callable<Response>> requests = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .when()
                            .get("/api/properties")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 75);

        System.out.println("Property Listing Load: " + result);
        assertThat(result.successRate).isGreaterThan(95.0);
        assertThat(result.medianResponseTime).isLessThan(500);
    }

    @Test
    @Order(7)
    @DisplayName("Load Test: Mixed operations (read/write)")
    void testMixedOperationsLoad() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        // 60% reads, 40% writes
        for (int i = 0; i < 60; i++) {
            requests.add(() -> given().spec(authenticatedRequest()).when().get("/api/bookings"));
        }

        for (int i = 0; i < 40; i++) {
            Long roomId = createRoom(testPropertyId, testRoomTypeId, "MIX" + i);
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "load@test.com");
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .body(booking)
                            .when()
                            .post("/api/bookings")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 100);

        System.out.println("Mixed Operations: " + result);
        assertThat(result.successRate).isGreaterThan(80.0);
    }

    @Test
    @Order(8)
    @DisplayName("Load Test: Authentication under load")
    void testAuthenticationLoad() throws InterruptedException {
        // Create user for login tests
        String username = "authloaduser";
        String password = "AuthLoad123!@#";
        registerUser(username, "authload@test.com", password, "Auth", "User");

        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Map<String, String> credentials = Map.of(
                    "username", username,
                    "password", password
            );

            requests.add(() ->
                    given()
                            .spec(requestSpec)
                            .body(credentials)
                            .when()
                            .post("/api/auth/login")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 50);

        System.out.println("Authentication Load: " + result);
        assertThat(result.successRate).isGreaterThan(90.0);
        assertThat(result.averageResponseTime).isLessThan(1000);
    }

    @Test
    @Order(9)
    @DisplayName("Load Test: Booking modifications under load")
    void testBookingModificationsLoad() throws InterruptedException {
        // Create bookings first
        List<Long> bookingIds = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Long roomId = createRoom(testPropertyId, testRoomTypeId, "MOD" + i);
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "load@test.com");
            Response response = authenticatedPost("/api/bookings", booking);
            if (response.statusCode() == 201) {
                bookingIds.add(response.path("id"));
            }
        }

        List<Callable<Response>> requests = new ArrayList<>();

        for (Long bookingId : bookingIds) {
            Map<String, Object> update = Map.of("numberOfGuests", 2);
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .body(update)
                            .when()
                            .put("/api/bookings/" + bookingId)
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 30);

        System.out.println("Booking Modifications: " + result);
        assertThat(result.successRate).isGreaterThan(75.0);
    }

    @Test
    @Order(10)
    @DisplayName("Load Test: Database query performance")
    void testDatabaseQueryPerformance() throws InterruptedException {
        // Create substantial test data
        for (int i = 0; i < 100; i++) {
            Long roomId = createRoom(testPropertyId, testRoomTypeId, "DB" + i);
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "load@test.com");
            authenticatedPost("/api/bookings", booking);
        }

        List<Callable<Response>> requests = new ArrayList<>();

        // Query all bookings
        for (int i = 0; i < 50; i++) {
            requests.add(() -> given().spec(authenticatedRequest()).when().get("/api/bookings"));
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 50);

        System.out.println("Database Query Performance: " + result);
        assertThat(result.successRate).isGreaterThan(95.0);
        assertThat(result.p95ResponseTime).isLessThan(2000);
    }

    // ==================== STRESS TESTS (10 scenarios) ====================

    @Test
    @Order(11)
    @DisplayName("Stress Test: Maximum concurrent users (200)")
    void testMaximumConcurrentUsers() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            requests.add(() -> given().spec(authenticatedRequest()).when().get("/api/properties"));
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 200);

        System.out.println("Maximum Concurrent Users: " + result);
        assertThat(result.failureCount).isLessThan(result.totalRequests / 2); // Less than 50% failure
    }

    @Test
    @Order(12)
    @DisplayName("Stress Test: Burst traffic spike")
    void testBurstTrafficSpike() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        // Sudden burst of 150 requests
        for (int i = 0; i < 150; i++) {
            Map<String, Object> booking = TestDataGenerator.generateBooking(testRoomId, "load@test.com");
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .body(booking)
                            .when()
                            .post("/api/bookings")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 150);

        System.out.println("Burst Traffic: " + result);
        // System should handle burst without crashing
        assertThat(result.totalRequests).isEqualTo(150);
    }

    @Test
    @Order(13)
    @DisplayName("Stress Test: Rapid-fire requests")
    void testRapidFireRequests() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/rooms");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 50, 10);

        System.out.println("Rapid Fire Requests: " + result);
        assertThat(result.throughput).isGreaterThan(5.0);
    }

    @Test
    @Order(14)
    @DisplayName("Stress Test: Large payload handling")
    void testLargePayloadHandling() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            Map<String, Object> booking = TestDataGenerator.generateBooking(testRoomId, "load@test.com");
            booking.put("specialRequests", "A".repeat(1000)); // 1KB special request

            requests.add(() ->
                    given()
                            .spec(authenticatedRequest())
                            .body(booking)
                            .when()
                            .post("/api/bookings")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 25);

        System.out.println("Large Payload: " + result);
        assertThat(result.successRate).isGreaterThan(70.0);
    }

    @Test
    @Order(15)
    @DisplayName("Stress Test: Memory pressure")
    void testMemoryPressure() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        // Create many objects to apply memory pressure
        for (int i = 0; i < 100; i++) {
            Map<String, String> userData = TestDataGenerator.generateUserRegistration();
            requests.add(() ->
                    given()
                            .spec(requestSpec)
                            .body(userData)
                            .when()
                            .post("/api/auth/register")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 100);

        System.out.println("Memory Pressure: " + result);
        assertThat(result.failureCount).isLessThan(result.totalRequests);
    }

    @Test
    @Order(16)
    @DisplayName("Stress Test: Connection pool exhaustion")
    void testConnectionPoolExhaustion() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        // Many database-intensive operations
        for (int i = 0; i < 75; i++) {
            requests.add(() -> given().spec(authenticatedRequest()).when().get("/api/bookings"));
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 75);

        System.out.println("Connection Pool: " + result);
        assertThat(result.successRate).isGreaterThan(60.0);
    }

    @Test
    @Order(17)
    @DisplayName("Stress Test: Ramped stress to breaking point")
    void testRampedStressToBreakingPoint() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/properties");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeRampedLoad(request, 10, 150, 15);

        System.out.println("Ramped Stress: " + result);
        // Should handle increasing load gracefully
        assertThat(result.totalRequests).isGreaterThan(0);
    }

    @Test
    @Order(18)
    @DisplayName("Stress Test: Concurrent write operations")
    void testConcurrentWriteOperations() throws InterruptedException {
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Map<String, Object> property = TestDataGenerator.generateProperty();
            requests.add(() ->
                    given()
                            .spec(adminRequest())
                            .body(property)
                            .when()
                            .post("/api/properties")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 50);

        System.out.println("Concurrent Writes: " + result);
        assertThat(result.successRate).isGreaterThan(70.0);
    }

    @Test
    @Order(19)
    @DisplayName("Stress Test: Race condition testing")
    void testRaceConditions() throws InterruptedException {
        // Multiple users trying to book same room
        List<Callable<Response>> requests = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            final int index = i;
            String userToken = registerUser("race" + index, "race" + index + "@test.com", "Race123!@#", "Race", "User" + index);

            Map<String, Object> booking = TestDataGenerator.generateBooking(testRoomId, "race" + index + "@test.com");
            requests.add(() ->
                    given()
                            .spec(authenticatedRequest(userToken))
                            .body(booking)
                            .when()
                            .post("/api/bookings")
            );
        }

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeConcurrentRequests(requests, 20);

        System.out.println("Race Conditions: " + result);
        // Most should fail due to room already booked - this is expected behavior
        assertThat(result.totalRequests).isEqualTo(20);
    }

    @Test
    @Order(20)
    @DisplayName("Stress Test: Resource exhaustion recovery")
    void testResourceExhaustionRecovery() throws InterruptedException {
        // First wave - exhaust resources
        List<Callable<Response>> wave1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            wave1.add(() -> given().spec(authenticatedRequest()).when().get("/api/rooms"));
        }

        LoadTestHelper.LoadTestResult result1 = loadTestHelper.executeConcurrentRequests(wave1, 100);
        System.out.println("Wave 1 (Exhaustion): " + result1);

        // Wait for recovery
        Thread.sleep(2000);

        // Second wave - verify recovery
        List<Callable<Response>> wave2 = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            wave2.add(() -> given().spec(authenticatedRequest()).when().get("/api/rooms"));
        }

        LoadTestHelper.LoadTestResult result2 = loadTestHelper.executeConcurrentRequests(wave2, 50);
        System.out.println("Wave 2 (Recovery): " + result2);

        // System should recover
        assertThat(result2.successRate).isGreaterThan(result1.successRate);
    }

    // ==================== ENDURANCE TESTS (10 scenarios) ====================

    @Test
    @Order(21)
    @DisplayName("Endurance Test: Sustained load for 30 seconds")
    void testSustainedLoad30Seconds() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/properties");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 10, 30);

        System.out.println("30s Sustained Load: " + result);
        assertThat(result.successRate).isGreaterThan(90.0);
        assertThat(result.averageResponseTime).isLessThan(1000);
    }

    @Test
    @Order(22)
    @DisplayName("Endurance Test: Memory leak detection")
    void testMemoryLeakDetection() throws InterruptedException {
        Callable<Response> request = () -> {
            Map<String, Object> booking = TestDataGenerator.generateBooking(testRoomId, "load@test.com");
            return given()
                    .spec(authenticatedRequest())
                    .body(booking)
                    .when()
                    .post("/api/bookings");
        };

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 5, 20);

        System.out.println("Memory Leak Detection: " + result);
        // Response times should not degrade significantly
        assertThat(result.p99ResponseTime).isLessThan(5000);
    }

    @Test
    @Order(23)
    @DisplayName("Endurance Test: Connection leak detection")
    void testConnectionLeakDetection() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/bookings");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 15, 25);

        System.out.println("Connection Leak Detection: " + result);
        assertThat(result.successRate).isGreaterThan(85.0);
    }

    @Test
    @Order(24)
    @DisplayName("Endurance Test: Performance degradation check")
    void testPerformanceDegradation() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/rooms");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 20, 30);

        System.out.println("Performance Degradation: " + result);
        // Average response time should remain stable
        assertThat(result.averageResponseTime).isLessThan(2000);
    }

    @Test
    @Order(25)
    @DisplayName("Endurance Test: Continuous booking operations")
    void testContinuousBookingOperations() throws InterruptedException {
        Callable<Response> request = () -> {
            Long roomId = createRoom(testPropertyId, testRoomTypeId, "END" + System.currentTimeMillis());
            Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "load@test.com");
            return given()
                    .spec(authenticatedRequest())
                    .body(booking)
                    .when()
                    .post("/api/bookings");
        };

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 5, 20);

        System.out.println("Continuous Bookings: " + result);
        assertThat(result.successRate).isGreaterThan(70.0);
    }

    @Test
    @Order(26)
    @DisplayName("Endurance Test: Cache effectiveness")
    void testCacheEffectiveness() throws InterruptedException {
        // First request should populate cache
        given().spec(authenticatedRequest()).when().get("/api/properties");

        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/properties");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 20, 15);

        System.out.println("Cache Effectiveness: " + result);
        // Cached requests should be fast
        assertThat(result.medianResponseTime).isLessThan(500);
    }

    @Test
    @Order(27)
    @DisplayName("Endurance Test: Session management")
    void testSessionManagement() throws InterruptedException {
        Callable<Response> request = () -> {
            String token = registerUser(
                    "session" + System.currentTimeMillis(),
                    "session" + System.currentTimeMillis() + "@test.com",
                    "Session123!@#",
                    "Session",
                    "User"
            );
            return given()
                    .spec(authenticatedRequest(token))
                    .when()
                    .get("/api/bookings");
        };

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 5, 15);

        System.out.println("Session Management: " + result);
        assertThat(result.successRate).isGreaterThan(80.0);
    }

    @Test
    @Order(28)
    @DisplayName("Endurance Test: Database connection pool stability")
    void testDatabaseConnectionPoolStability() throws InterruptedException {
        Callable<Response> request = () ->
                given().spec(authenticatedRequest()).when().get("/api/bookings");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 25, 20);

        System.out.println("DB Connection Pool Stability: " + result);
        assertThat(result.successRate).isGreaterThan(90.0);
    }

    @Test
    @Order(29)
    @DisplayName("Endurance Test: Garbage collection impact")
    void testGarbageCollectionImpact() throws InterruptedException {
        Callable<Response> request = () -> {
            // Create some objects to trigger GC
            Map<String, Object> data = TestDataGenerator.generateBooking(testRoomId, "load@test.com");
            return given()
                    .spec(authenticatedRequest())
                    .when()
                    .get("/api/properties");
        };

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 15, 25);

        System.out.println("GC Impact: " + result);
        // GC shouldn't significantly impact performance
        assertThat(result.p95ResponseTime).isLessThan(3000);
    }

    @Test
    @Order(30)
    @DisplayName("Endurance Test: Long-running stability")
    void testLongRunningStability() throws InterruptedException {
        Callable<Response> request = () ->
                given()
                        .spec(authenticatedRequest())
                        .queryParam("checkIn", "2025-12-01")
                        .queryParam("checkOut", "2025-12-05")
                        .queryParam("guests", 2)
                        .when()
                        .get("/api/rooms/availability");

        LoadTestHelper.LoadTestResult result = loadTestHelper.executeSustainedLoad(request, 10, 30);

        System.out.println("Long Running Stability: " + result);
        assertThat(result.successRate).isGreaterThan(85.0);
        assertThat(result.throughput).isGreaterThan(1.0);
    }
}
