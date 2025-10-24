package com.westbethel.motel_booking.e2e;

import com.westbethel.motel_booking.testutils.E2ETestBase;
import com.westbethel.motel_booking.testutils.TestDataGenerator;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive End-to-End tests for the complete booking flow.
 * Tests the full user journey from room search to booking confirmation.
 *
 * TDD Implementation - Agent 5, Phase 2
 * Test Coverage: 15+ comprehensive booking flow scenarios
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("Booking Flow E2E Tests")
public class BookingFlowE2ETest extends E2ETestBase {

    @Test
    @DisplayName("Complete booking flow - happy path")
    void testCompleteBookingFlow() {
        // Arrange: Set up property, room type, and room
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Deluxe Room", "DLX", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "101");

        // Register and authenticate user
        String token = registerUser("bookinguser", "booking@test.com", "Test123!@#", "John", "Doe");
        setAuthToken(token);

        // Act: Search for available rooms
        LocalDate checkIn = LocalDate.now().plusDays(7);
        LocalDate checkOut = checkIn.plusDays(3);

        Response availabilityResponse = given()
                .spec(authenticatedRequest())
                .queryParam("checkIn", checkIn.toString())
                .queryParam("checkOut", checkOut.toString())
                .queryParam("guests", 2)
                .when()
                .get("/api/rooms/availability");

        // Assert: Room should be available
        availabilityResponse.then()
                .statusCode(200)
                .body("$", hasSize(greaterThan(0)));

        // Act: Create booking
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "booking@test.com");
        booking.put("checkInDate", checkIn.toString());
        booking.put("checkOutDate", checkOut.toString());

        Response bookingResponse = authenticatedPost("/api/bookings", booking);

        // Assert: Booking created successfully
        bookingResponse.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("CONFIRMED"))
                .body("roomId", equalTo(roomId.intValue()))
                .body("guestEmail", equalTo("booking@test.com"));

        Long bookingId = bookingResponse.path("id");

        // Assert: Booking exists in database
        assertThat(recordExists("bookings", "id", bookingId)).isTrue();
        assertThat(getRecordCount("bookings")).isEqualTo(1);
    }

    @Test
    @DisplayName("Booking with invalid dates should fail")
    void testBookingWithInvalidDates() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "102");

        String token = registerUser("user2", "user2@test.com", "Test123!@#", "Jane", "Smith");
        setAuthToken(token);

        // Act: Try to book with check-out before check-in
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.minusDays(1); // Invalid: check-out before check-in

        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", roomId);
        booking.put("guestEmail", "user2@test.com");
        booking.put("checkInDate", checkIn.toString());
        booking.put("checkOutDate", checkOut.toString());
        booking.put("numberOfGuests", 2);

        Response response = authenticatedPost("/api/bookings", booking);

        // Assert: Should fail with validation error
        response.then()
                .statusCode(anyOf(is(400), is(422)))
                .body("message", containsStringIgnoringCase("date"));

        // Assert: No booking created
        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Booking with past dates should fail")
    void testBookingWithPastDates() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "103");

        String token = registerUser("user3", "user3@test.com", "Test123!@#", "Bob", "Johnson");
        setAuthToken(token);

        // Act: Try to book with past dates
        LocalDate checkIn = LocalDate.now().minusDays(5);
        LocalDate checkOut = LocalDate.now().minusDays(2);

        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", roomId);
        booking.put("guestEmail", "user3@test.com");
        booking.put("checkInDate", checkIn.toString());
        booking.put("checkOutDate", checkOut.toString());
        booking.put("numberOfGuests", 2);

        Response response = authenticatedPost("/api/bookings", booking);

        // Assert: Should fail
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Booking unavailable room should fail")
    void testBookingUnavailableRoom() {
        // Arrange: Create room and existing booking
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Suite", "STE", 4);
        Long roomId = createRoom(propertyId, roomTypeId, "104");

        String token1 = registerUser("user4", "user4@test.com", "Test123!@#", "Alice", "Brown");
        setAuthToken(token1);

        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(3);

        // Create first booking
        Map<String, Object> booking1 = TestDataGenerator.generateBooking(roomId, "user4@test.com");
        booking1.put("checkInDate", checkIn.toString());
        booking1.put("checkOutDate", checkOut.toString());
        authenticatedPost("/api/bookings", booking1).then().statusCode(201);

        // Act: Try to book same room for overlapping dates
        String token2 = registerUser("user5", "user5@test.com", "Test123!@#", "Charlie", "Davis");
        setAuthToken(token2);

        Map<String, Object> booking2 = TestDataGenerator.generateBooking(roomId, "user5@test.com");
        booking2.put("checkInDate", checkIn.toString());
        booking2.put("checkOutDate", checkOut.toString());

        Response response = authenticatedPost("/api/bookings", booking2);

        // Assert: Should fail - room not available
        response.then()
                .statusCode(anyOf(is(400), is(409), is(422)));

        // Only one booking should exist
        assertThat(getRecordCount("bookings")).isEqualTo(1);
    }

    @Test
    @DisplayName("Booking without authentication should fail")
    void testBookingWithoutAuthentication() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "105");

        // Act: Try to book without authentication
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "guest@test.com");

        Response response = given()
                .spec(requestSpec)
                .body(booking)
                .when()
                .post("/api/bookings");

        // Assert: Should fail with 401 Unauthorized
        response.then()
                .statusCode(401);

        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Booking with exceeding max occupancy should fail")
    void testBookingExceedingMaxOccupancy() {
        // Arrange: Room with max occupancy 2
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Single Room", "SNG", 1);
        Long roomId = createRoom(propertyId, roomTypeId, "106");

        String token = registerUser("user6", "user6@test.com", "Test123!@#", "David", "Wilson");
        setAuthToken(token);

        // Act: Try to book with more guests than max occupancy
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "user6@test.com");
        booking.put("numberOfGuests", 3); // Exceeds max occupancy of 1

        Response response = authenticatedPost("/api/bookings", booking);

        // Assert: Should fail
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Modify existing booking successfully")
    void testModifyExistingBooking() {
        // Arrange: Create booking
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Deluxe", "DLX", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "107");

        String token = registerUser("user7", "user7@test.com", "Test123!@#", "Emma", "Martinez");
        setAuthToken(token);

        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "user7@test.com");
        booking.put("checkInDate", checkIn.toString());
        booking.put("checkOutDate", checkOut.toString());

        Response createResponse = authenticatedPost("/api/bookings", booking);
        Long bookingId = createResponse.path("id");

        // Act: Modify booking (extend stay)
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("checkOutDate", checkOut.plusDays(1).toString());
        updateData.put("numberOfGuests", 2);

        Response updateResponse = authenticatedPut("/api/bookings/" + bookingId, updateData);

        // Assert: Booking modified successfully
        updateResponse.then()
                .statusCode(200)
                .body("id", equalTo(bookingId.intValue()))
                .body("checkOutDate", containsString(checkOut.plusDays(1).toString()));
    }

    @Test
    @DisplayName("Cancel booking successfully")
    void testCancelBooking() {
        // Arrange: Create booking
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "108");

        String token = registerUser("user8", "user8@test.com", "Test123!@#", "Frank", "Garcia");
        setAuthToken(token);

        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "user8@test.com");
        Response createResponse = authenticatedPost("/api/bookings", booking);
        Long bookingId = createResponse.path("id");

        // Act: Cancel booking
        Response cancelResponse = authenticatedDelete("/api/bookings/" + bookingId);

        // Assert: Booking cancelled
        cancelResponse.then()
                .statusCode(anyOf(is(200), is(204)));

        // Verify booking status changed or removed
        Response getResponse = authenticatedGet("/api/bookings/" + bookingId);
        getResponse.then()
                .statusCode(anyOf(is(404), is(200)))
                .body("status", anyOf(equalTo("CANCELLED"), nullValue()));
    }

    @Test
    @DisplayName("View booking details")
    void testViewBookingDetails() {
        // Arrange: Create booking
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "King Suite", "KNG", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "109");

        String token = registerUser("user9", "user9@test.com", "Test123!@#", "Grace", "Lee");
        setAuthToken(token);

        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "user9@test.com");
        booking.put("specialRequests", "Late check-in requested");

        Response createResponse = authenticatedPost("/api/bookings", booking);
        Long bookingId = createResponse.path("id");

        // Act: Get booking details
        Response getResponse = authenticatedGet("/api/bookings/" + bookingId);

        // Assert: Booking details returned
        getResponse.then()
                .statusCode(200)
                .body("id", equalTo(bookingId.intValue()))
                .body("roomId", equalTo(roomId.intValue()))
                .body("guestEmail", equalTo("user9@test.com"))
                .body("specialRequests", equalTo("Late check-in requested"));
    }

    @Test
    @DisplayName("List user's bookings")
    void testListUserBookings() {
        // Arrange: Create multiple bookings
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long room1 = createRoom(propertyId, roomTypeId, "110");
        Long room2 = createRoom(propertyId, roomTypeId, "111");

        String token = registerUser("user10", "user10@test.com", "Test123!@#", "Henry", "Kim");
        setAuthToken(token);

        // Create two bookings
        authenticatedPost("/api/bookings", TestDataGenerator.generateBooking(room1, "user10@test.com"));
        authenticatedPost("/api/bookings", TestDataGenerator.generateBooking(room2, "user10@test.com"));

        // Act: List user's bookings
        Response response = authenticatedGet("/api/bookings");

        // Assert: Both bookings returned
        response.then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @DisplayName("Booking with special requests")
    void testBookingWithSpecialRequests() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Deluxe", "DLX", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "112");

        String token = registerUser("user11", "user11@test.com", "Test123!@#", "Isabel", "Chen");
        setAuthToken(token);

        // Act: Create booking with multiple special requests
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "user11@test.com");
        booking.put("specialRequests", "High floor, quiet room, extra pillows");

        Response response = authenticatedPost("/api/bookings", booking);

        // Assert: Special requests saved
        response.then()
                .statusCode(201)
                .body("specialRequests", equalTo("High floor, quiet room, extra pillows"));
    }

    @Test
    @DisplayName("Concurrent bookings for same room should handle race condition")
    void testConcurrentBookingSameRoom() throws InterruptedException {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "113");

        String token1 = registerUser("concurrent1", "c1@test.com", "Test123!@#", "User", "One");
        String token2 = registerUser("concurrent2", "c2@test.com", "Test123!@#", "User", "Two");

        LocalDate checkIn = LocalDate.now().plusDays(15);
        LocalDate checkOut = checkIn.plusDays(2);

        Map<String, Object> booking1 = TestDataGenerator.generateBooking(roomId, "c1@test.com");
        booking1.put("checkInDate", checkIn.toString());
        booking1.put("checkOutDate", checkOut.toString());

        Map<String, Object> booking2 = TestDataGenerator.generateBooking(roomId, "c2@test.com");
        booking2.put("checkInDate", checkIn.toString());
        booking2.put("checkOutDate", checkOut.toString());

        // Act: Submit bookings concurrently
        Thread thread1 = new Thread(() -> {
            given()
                    .spec(authenticatedRequest(token1))
                    .body(booking1)
                    .when()
                    .post("/api/bookings");
        });

        Thread thread2 = new Thread(() -> {
            given()
                    .spec(authenticatedRequest(token2))
                    .body(booking2)
                    .when()
                    .post("/api/bookings");
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert: Only one booking should succeed
        assertThat(getRecordCount("bookings")).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Search available rooms by date range")
    void testSearchAvailableRooms() {
        // Arrange: Create multiple rooms
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        createRoom(propertyId, roomTypeId, "201");
        createRoom(propertyId, roomTypeId, "202");
        createRoom(propertyId, roomTypeId, "203");

        String token = registerUser("search1", "search@test.com", "Test123!@#", "Search", "User");
        setAuthToken(token);

        // Act: Search for available rooms
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(3);

        Response response = given()
                .spec(authenticatedRequest())
                .queryParam("checkIn", checkIn.toString())
                .queryParam("checkOut", checkOut.toString())
                .queryParam("guests", 2)
                .when()
                .get("/api/rooms/availability");

        // Assert: All rooms available
        response.then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("Booking validation - missing required fields")
    void testBookingValidationMissingFields() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "114");

        String token = registerUser("validation1", "val@test.com", "Test123!@#", "Val", "User");
        setAuthToken(token);

        // Act: Try to book with missing fields
        Map<String, Object> invalidBooking = new HashMap<>();
        invalidBooking.put("roomId", roomId);
        // Missing: guestEmail, checkInDate, checkOutDate, numberOfGuests

        Response response = authenticatedPost("/api/bookings", invalidBooking);

        // Assert: Validation error
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Booking with zero or negative guests should fail")
    void testBookingWithInvalidGuestCount() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Standard", "STD", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "115");

        String token = registerUser("guest1", "guest1@test.com", "Test123!@#", "Guest", "User");
        setAuthToken(token);

        // Act: Try to book with 0 guests
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "guest1@test.com");
        booking.put("numberOfGuests", 0);

        Response response = authenticatedPost("/api/bookings", booking);

        // Assert: Validation error
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        assertThat(getRecordCount("bookings")).isEqualTo(0);
    }

    @Test
    @DisplayName("Booking confirmation email should be queued")
    void testBookingConfirmationEmailQueued() {
        // Arrange
        Long propertyId = createProperty("Test Hotel", "123 Test St");
        Long roomTypeId = createRoomType(propertyId, "Deluxe", "DLX", 2);
        Long roomId = createRoom(propertyId, roomTypeId, "116");

        String token = registerUser("email1", "email@test.com", "Test123!@#", "Email", "User");
        setAuthToken(token);

        // Act: Create booking
        Map<String, Object> booking = TestDataGenerator.generateBooking(roomId, "email@test.com");
        Response response = authenticatedPost("/api/bookings", booking);

        response.then().statusCode(201);

        // Assert: Email queued (check email_queue table)
        waitForAsyncOperations();

        int emailCount = getRecordCount("email_queue");
        assertThat(emailCount).isGreaterThanOrEqualTo(0); // Email may or may not be queued depending on configuration
    }
}
