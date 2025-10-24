package com.westbethel.motel_booking.testutils;

import com.github.javafaker.Faker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating realistic test data for E2E, load, and chaos testing.
 * Uses JavaFaker for realistic data generation.
 *
 * TDD Implementation for Agent 5 - Phase 2
 */
public class TestDataGenerator {

    private static final Faker faker = new Faker();
    private static final Random random = new Random();

    // ==================== User Data Generation ====================

    public static Map<String, String> generateUserRegistration() {
        Map<String, String> user = new HashMap<>();
        user.put("username", generateUsername());
        user.put("email", generateEmail());
        user.put("password", generateSecurePassword());
        user.put("firstName", faker.name().firstName());
        user.put("lastName", faker.name().lastName());
        return user;
    }

    public static String generateUsername() {
        return faker.name().username() + randomInt(1000, 9999);
    }

    public static String generateEmail() {
        return "test" + randomInt(10000, 99999) + "@example.com";
    }

    public static String generateSecurePassword() {
        return "Test123!@#" + randomInt(100, 999);
    }

    public static String generateWeakPassword() {
        return "password";
    }

    // ==================== Property Data Generation ====================

    public static Map<String, Object> generateProperty() {
        Map<String, Object> property = new HashMap<>();
        property.put("name", faker.company().name() + " Hotel");
        property.put("address", faker.address().streetAddress());
        property.put("city", faker.address().city());
        property.put("state", faker.address().stateAbbr());
        property.put("country", "USA");
        property.put("zipCode", faker.address().zipCode());
        property.put("phone", faker.phoneNumber().phoneNumber());
        property.put("email", faker.internet().emailAddress());
        return property;
    }

    // ==================== Room Type Data Generation ====================

    public static Map<String, Object> generateRoomType(Long propertyId) {
        String[] roomTypes = {"SINGLE", "DOUBLE", "SUITE", "DELUXE", "KING"};
        String type = roomTypes[random.nextInt(roomTypes.length)];

        Map<String, Object> roomType = new HashMap<>();
        roomType.put("propertyId", propertyId);
        roomType.put("name", type + " Room");
        roomType.put("code", type.substring(0, 3).toUpperCase());
        roomType.put("description", "Comfortable " + type.toLowerCase() + " room");
        roomType.put("maxOccupancy", randomInt(1, 4));
        roomType.put("baseRate", randomBigDecimal(50, 500));
        return roomType;
    }

    // ==================== Room Data Generation ====================

    public static Map<String, Object> generateRoom(Long propertyId, Long roomTypeId) {
        Map<String, Object> room = new HashMap<>();
        room.put("propertyId", propertyId);
        room.put("roomTypeId", roomTypeId);
        room.put("roomNumber", String.valueOf(randomInt(100, 999)));
        room.put("floor", randomInt(1, 10));
        room.put("status", "AVAILABLE");
        return room;
    }

    // ==================== Guest Data Generation ====================

    public static Map<String, Object> generateGuest() {
        Map<String, Object> guest = new HashMap<>();
        guest.put("firstName", faker.name().firstName());
        guest.put("lastName", faker.name().lastName());
        guest.put("email", generateEmail());
        guest.put("phone", faker.phoneNumber().phoneNumber());
        guest.put("address", faker.address().streetAddress());
        guest.put("city", faker.address().city());
        guest.put("state", faker.address().stateAbbr());
        guest.put("zipCode", faker.address().zipCode());
        guest.put("country", "USA");
        return guest;
    }

    // ==================== Booking Data Generation ====================

    public static Map<String, Object> generateBooking(Long roomId, String guestEmail) {
        LocalDate checkIn = generateFutureDate(1, 30);
        LocalDate checkOut = checkIn.plusDays(randomInt(1, 7));

        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", roomId);
        booking.put("guestEmail", guestEmail);
        booking.put("checkInDate", checkIn.toString());
        booking.put("checkOutDate", checkOut.toString());
        booking.put("numberOfGuests", randomInt(1, 4));
        booking.put("specialRequests", generateSpecialRequest());
        return booking;
    }

    public static String generateSpecialRequest() {
        String[] requests = {
                "Late check-in requested",
                "Need extra pillows",
                "Prefer high floor",
                "Quiet room please",
                "Near elevator",
                null  // No special request
        };
        return requests[random.nextInt(requests.length)];
    }

    // ==================== Payment Data Generation ====================

    public static Map<String, Object> generatePayment(Long bookingId, BigDecimal amount) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("bookingId", bookingId);
        payment.put("amount", amount);
        payment.put("paymentMethod", generatePaymentMethod());
        payment.put("cardNumber", generateCreditCardNumber());
        payment.put("cardHolderName", faker.name().fullName());
        payment.put("expiryDate", generateExpiryDate());
        payment.put("cvv", String.valueOf(randomInt(100, 999)));
        return payment;
    }

    public static String generatePaymentMethod() {
        String[] methods = {"CREDIT_CARD", "DEBIT_CARD", "PAYPAL"};
        return methods[random.nextInt(methods.length)];
    }

    public static String generateCreditCardNumber() {
        // Generate test credit card number (not real)
        return "4532" + randomInt(10000000, 99999999) + "0000";
    }

    public static String generateExpiryDate() {
        int month = randomInt(1, 12);
        int year = LocalDate.now().getYear() + randomInt(1, 5);
        return String.format("%02d/%d", month, year);
    }

    // ==================== Date and Time Generation ====================

    public static LocalDate generateFutureDate(int minDays, int maxDays) {
        int days = randomInt(minDays, maxDays);
        return LocalDate.now().plusDays(days);
    }

    public static LocalDate generatePastDate(int minDays, int maxDays) {
        int days = randomInt(minDays, maxDays);
        return LocalDate.now().minusDays(days);
    }

    public static LocalDateTime generateFutureDateTime(int minHours, int maxHours) {
        int hours = randomInt(minHours, maxHours);
        return LocalDateTime.now().plusHours(hours);
    }

    // ==================== Rate Plan Data Generation ====================

    public static Map<String, Object> generateRatePlan(Long propertyId, Long roomTypeId) {
        Map<String, Object> ratePlan = new HashMap<>();
        ratePlan.put("propertyId", propertyId);
        ratePlan.put("roomTypeId", roomTypeId);
        ratePlan.put("name", generateRatePlanName());
        ratePlan.put("code", "RATE" + randomInt(100, 999));
        ratePlan.put("baseRate", randomBigDecimal(50, 500));
        ratePlan.put("minStay", randomInt(1, 3));
        ratePlan.put("maxStay", randomInt(7, 30));
        ratePlan.put("validFrom", LocalDate.now().toString());
        ratePlan.put("validTo", generateFutureDate(30, 365).toString());
        return ratePlan;
    }

    public static String generateRatePlanName() {
        String[] names = {
                "Standard Rate",
                "Weekend Special",
                "Early Bird Discount",
                "Last Minute Deal",
                "Extended Stay"
        };
        return names[random.nextInt(names.length)];
    }

    // ==================== Malicious Input Generation (for security testing) ====================

    public static String generateSqlInjectionPayload() {
        String[] payloads = {
                "' OR '1'='1",
                "'; DROP TABLE users--",
                "' UNION SELECT * FROM users--",
                "admin'--",
                "' OR 1=1--"
        };
        return payloads[random.nextInt(payloads.length)];
    }

    public static String generateXssPayload() {
        String[] payloads = {
                "<script>alert('XSS')</script>",
                "<img src=x onerror=alert('XSS')>",
                "<svg onload=alert('XSS')>",
                "javascript:alert('XSS')",
                "<iframe src='javascript:alert(\"XSS\")'>"
        };
        return payloads[random.nextInt(payloads.length)];
    }

    public static String generateCommandInjectionPayload() {
        String[] payloads = {
                "; ls -la",
                "| cat /etc/passwd",
                "`whoami`",
                "$(rm -rf /)",
                "&& cat /etc/shadow"
        };
        return payloads[random.nextInt(payloads.length)];
    }

    public static String generatePathTraversalPayload() {
        String[] payloads = {
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32",
                "....//....//....//etc/passwd",
                "..;/..;/..;/etc/passwd"
        };
        return payloads[random.nextInt(payloads.length)];
    }

    public static Map<String, String> generateMaliciousHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "127.0.0.1' OR '1'='1");
        headers.put("User-Agent", "<script>alert('XSS')</script>");
        headers.put("Referer", "javascript:alert('XSS')");
        return headers;
    }

    // ==================== Load Testing Data Generation ====================

    public static List<Map<String, Object>> generateBulkBookings(int count, Long roomId, String guestEmail) {
        List<Map<String, Object>> bookings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bookings.add(generateBooking(roomId, guestEmail));
        }
        return bookings;
    }

    public static List<Map<String, String>> generateBulkUsers(int count) {
        List<Map<String, String>> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(generateUserRegistration());
        }
        return users;
    }

    public static List<Map<String, Object>> generateBulkProperties(int count) {
        List<Map<String, Object>> properties = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            properties.add(generateProperty());
        }
        return properties;
    }

    // ==================== Utility Methods ====================

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static BigDecimal randomBigDecimal(double min, double max) {
        double value = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }

    public static <T> T randomElement(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String randomAlphanumeric(int length) {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, length);
    }

    // ==================== Chaos Engineering Data ====================

    public static Map<String, Object> generateCorruptedBooking() {
        Map<String, Object> booking = new HashMap<>();
        booking.put("roomId", null); // Corrupted data
        booking.put("guestEmail", "invalid-email"); // Invalid format
        booking.put("checkInDate", "invalid-date"); // Invalid date
        booking.put("checkOutDate", null); // Missing required field
        booking.put("numberOfGuests", -1); // Invalid value
        return booking;
    }

    public static Map<String, Object> generateIncompletePayment() {
        Map<String, Object> payment = new HashMap<>();
        payment.put("amount", null); // Missing amount
        payment.put("paymentMethod", "INVALID_METHOD"); // Invalid method
        return payment;
    }

    // ==================== Performance Testing Data ====================

    public static List<Map<String, Object>> generateConcurrentBookingRequests(int count) {
        List<Map<String, Object>> requests = new ArrayList<>();
        LocalDate checkIn = generateFutureDate(1, 30);
        LocalDate checkOut = checkIn.plusDays(3);

        for (int i = 0; i < count; i++) {
            Map<String, Object> booking = new HashMap<>();
            booking.put("roomId", 1L); // Same room - testing race conditions
            booking.put("guestEmail", "guest" + i + "@test.com");
            booking.put("checkInDate", checkIn.toString());
            booking.put("checkOutDate", checkOut.toString());
            booking.put("numberOfGuests", 2);
            requests.add(booking);
        }
        return requests;
    }
}
