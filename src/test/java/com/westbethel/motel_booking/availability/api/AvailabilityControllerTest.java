package com.westbethel.motel_booking.availability.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.westbethel.motel_booking.common.model.BookingChannel;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.testutil.BaseIntegrationTest;
import com.westbethel.motel_booking.testutil.TestDataBuilder;
import com.westbethel.motel_booking.testutil.TestDataBuilder.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for AvailabilityController.
 * Tests availability search functionality including date filtering, room type filtering,
 * and various edge cases.
 *
 * Test Coverage:
 * - Basic availability search with valid parameters
 * - Filtering by room type codes
 * - Search without room type filters (all types)
 * - Date validation (past dates, invalid ranges)
 * - No availability scenarios (fully booked)
 * - Multiple room types with different availability
 * - Adult/children capacity validation
 */
@DisplayName("Availability Controller Integration Tests")
class AvailabilityControllerTest extends BaseIntegrationTest {

    private Property property;
    private RoomType kingRoomType;
    private RoomType queenRoomType;
    private RoomType suiteRoomType;
    private RatePlan standardRatePlan;

    @BeforeEach
    void setup() {
        // Arrange: Create test property and room types
        property = propertyRepository.save(TestDataBuilder.createProperty());

        kingRoomType = roomTypeRepository.save(
                TestDataBuilder.createRoomType(property.getId(), "KING", "King Room",
                                              2, new BigDecimal("175.00")));

        queenRoomType = roomTypeRepository.save(
                TestDataBuilder.createRoomType(property.getId(), "QUEEN", "Queen Room",
                                               2, new BigDecimal("150.00")));

        suiteRoomType = roomTypeRepository.save(
                TestDataBuilder.createRoomType(property.getId(), "SUITE", "Suite",
                                               4, new BigDecimal("250.00")));

        // Create rooms for each type
        roomRepository.saveAll(List.of(
                TestDataBuilder.createRoom(property.getId(), kingRoomType.getId(), "101"),
                TestDataBuilder.createRoom(property.getId(), kingRoomType.getId(), "102"),
                TestDataBuilder.createRoom(property.getId(), kingRoomType.getId(), "103"),
                TestDataBuilder.createRoom(property.getId(), queenRoomType.getId(), "201"),
                TestDataBuilder.createRoom(property.getId(), queenRoomType.getId(), "202"),
                TestDataBuilder.createRoom(property.getId(), suiteRoomType.getId(), "301")
        ));

        standardRatePlan = ratePlanRepository.save(
                TestDataBuilder.createRatePlan(property.getId(), "Standard", BookingChannel.DIRECT,
                                              new BigDecimal("175.00"),
                                              Set.of(kingRoomType.getId(), queenRoomType.getId(),
                                                    suiteRoomType.getId())));
    }

    @Nested
    @DisplayName("Successful Availability Searches")
    class SuccessfulSearches {

        @Test
        @DisplayName("Should return availability for specific room type with all rooms available")
        void returnsAvailabilityForSpecificRoomType() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("KING"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(3))
                    .andExpect(jsonPath("$.roomTypes[0].nightlyRates").isArray())
                    .andExpect(jsonPath("$.roomTypes[0].nightlyRates", hasSize(2)));
        }

        @Test
        @DisplayName("Should return availability for all room types when no filter specified")
        void returnsAvailabilityForAllRoomTypes() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(10, 3);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes", hasSize(3)))
                    .andExpect(jsonPath("$.roomTypes[?(@.roomTypeCode == 'KING')].availableRooms").value(3))
                    .andExpect(jsonPath("$.roomTypes[?(@.roomTypeCode == 'QUEEN')].availableRooms").value(2))
                    .andExpect(jsonPath("$.roomTypes[?(@.roomTypeCode == 'SUITE')].availableRooms").value(1));
        }

        @Test
        @DisplayName("Should return availability for multiple specified room types")
        void returnsAvailabilityForMultipleRoomTypes() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(5, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING")
                            .param("roomTypes", "QUEEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes", hasSize(2)))
                    .andExpect(jsonPath("$.roomTypes[?(@.roomTypeCode == 'KING')].availableRooms").value(3))
                    .andExpect(jsonPath("$.roomTypes[?(@.roomTypeCode == 'QUEEN')].availableRooms").value(2));
        }

        @Test
        @DisplayName("Should return correct availability with default adults and children")
        void returnsAvailabilityWithDefaultGuestCounts() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(14, 1);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes").isArray());
        }

        @Test
        @DisplayName("Should return availability for family size room with children")
        void returnsAvailabilityWithChildren() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("children", "2")
                            .param("roomTypes", "SUITE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("SUITE"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(1));
        }
    }

    @Nested
    @DisplayName("Limited Availability Scenarios")
    class LimitedAvailabilityScenarios {

        @Test
        @DisplayName("Should return reduced availability when some rooms are booked")
        void returnsReducedAvailabilityWhenPartiallyBooked() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);
            var guest = guestRepository.save(TestDataBuilder.createGuest());

            // Create a booking that occupies one KING room
            Booking booking = TestDataBuilder.createBooking(
                    property.getId(), guest.getId(), standardRatePlan.getId(),
                    dateRange.start, dateRange.end,
                    new BigDecimal("350.00"));
            bookingRepository.save(booking);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("KING"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(2));
        }

        @Test
        @DisplayName("Should return zero availability when all rooms are booked")
        void returnsNoAvailabilityWhenFullyBooked() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);
            var guest = guestRepository.save(TestDataBuilder.createGuest());

            // Create bookings for all QUEEN rooms (2 total)
            for (int i = 0; i < 2; i++) {
                Booking booking = TestDataBuilder.createBooking(
                        property.getId(), guest.getId(), standardRatePlan.getId(),
                        dateRange.start, dateRange.end,
                        new BigDecimal("300.00"));
                bookingRepository.save(booking);
            }

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "QUEEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("QUEEN"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(0));
        }

        @Test
        @DisplayName("Should not count cancelled bookings against availability")
        void excludesCancelledBookingsFromAvailability() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);
            var guest = guestRepository.save(TestDataBuilder.createGuest());

            // Create a cancelled booking
            Booking cancelledBooking = TestDataBuilder.createBooking(
                    property.getId(), guest.getId(), standardRatePlan.getId(),
                    dateRange.start, dateRange.end,
                    new BigDecimal("350.00"),
                    BookingStatus.CANCELLED,
                    PaymentStatus.REFUNDED);
            bookingRepository.save(cancelledBooking);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("KING"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(3));
        }

        @Test
        @DisplayName("Should return correct availability for overlapping date ranges")
        void handlesOverlappingDateRanges() throws Exception {
            // Arrange
            var guest = guestRepository.save(TestDataBuilder.createGuest());

            // Existing booking: Day 7-9
            DateRange existingRange = DateRange.futureRange(7, 2);
            Booking existingBooking = TestDataBuilder.createBooking(
                    property.getId(), guest.getId(), standardRatePlan.getId(),
                    existingRange.start, existingRange.end,
                    new BigDecimal("350.00"));
            bookingRepository.save(existingBooking);

            // Search: Day 8-10 (overlaps with existing)
            DateRange searchRange = DateRange.futureRange(8, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", searchRange.start.toString())
                            .param("endDate", searchRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("KING"))
                    .andExpect(jsonPath("$.roomTypes[0].availableRooms").value(2));
        }
    }

    @Nested
    @DisplayName("Validation and Error Cases")
    class ValidationAndErrorCases {

        @Test
        @DisplayName("Should reject request with missing propertyId")
        void rejectsMissingPropertyId() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with missing start date")
        void rejectsMissingStartDate() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with missing end date")
        void rejectsMissingEndDate() throws Exception {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(7);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", startDate.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with invalid date format")
        void rejectsInvalidDateFormat() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", "2024-13-01")
                            .param("endDate", "2024-13-05")
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with end date before start date")
        void rejectsEndDateBeforeStartDate() throws Exception {
            // Arrange
            LocalDate startDate = LocalDate.now().plusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(5);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request with invalid propertyId format")
        void rejectsInvalidPropertyIdFormat() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", "not-a-uuid")
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle non-existent property gracefully")
        void handlesNonExistentProperty() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", java.util.UUID.randomUUID().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes", hasSize(0)));
        }

        @Test
        @DisplayName("Should handle non-existent room type filter gracefully")
        void handlesNonExistentRoomType() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "NONEXISTENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle single night stay")
        void handlesSingleNightStay() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 1);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].nightlyRates", hasSize(1)));
        }

        @Test
        @DisplayName("Should handle extended stay (30 nights)")
        void handlesExtendedStay() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 30);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "2")
                            .param("roomTypes", "KING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].nightlyRates", hasSize(30)));
        }

        @Test
        @DisplayName("Should handle same day check-in check-out")
        void handlesSameDayCheckInCheckOut() throws Exception {
            // Arrange
            LocalDate date = LocalDate.now().plusDays(7);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", date.toString())
                            .param("endDate", date.toString())
                            .param("adults", "2"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle maximum adult capacity search")
        void handlesMaximumAdultCapacity() throws Exception {
            // Arrange
            DateRange dateRange = DateRange.futureRange(7, 2);

            // Act & Assert
            mockMvc.perform(get("/api/v1/availability")
                            .param("propertyId", property.getId().toString())
                            .param("startDate", dateRange.start.toString())
                            .param("endDate", dateRange.end.toString())
                            .param("adults", "4")
                            .param("roomTypes", "SUITE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomTypes[0].roomTypeCode").value("SUITE"));
        }
    }
}
