package com.westbethel.motel_booking.reservation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.guest.domain.Guest;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.reservation.api.dto.BookingCreateRequest;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.testutil.BaseIntegrationTest;
import com.westbethel.motel_booking.testutil.TestDataBuilder;
import com.westbethel.motel_booking.testutil.TestDataBuilder.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for BookingController.
 * Tests booking creation, cancellation, and validation flows.
 *
 * Test Coverage:
 * - Create booking with valid data
 * - Cancel booking with valid confirmation number
 * - Validation errors (missing fields, invalid dates, etc.)
 * - Business rule violations (past dates, no availability, etc.)
 * - Complete booking lifecycle (create -> cancel)
 * - Edge cases (same-day bookings, extended stays, etc.)
 */
@DisplayName("Booking Controller Integration Tests")
class BookingControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    private Property property;
    private RoomType roomType;
    private Guest guest;
    private RatePlan ratePlan;

    @BeforeEach
    void setup() {
        // Arrange: Create test data
        property = propertyRepository.save(TestDataBuilder.createProperty());

        roomType = roomTypeRepository.save(
                TestDataBuilder.createRoomType(property.getId(), "KING", "King Room",
                                              2, new BigDecimal("175.00")));

        roomRepository.saveAll(List.of(
                TestDataBuilder.createRoom(property.getId(), roomType.getId(), "101"),
                TestDataBuilder.createRoom(property.getId(), roomType.getId(), "102")
        ));

        guest = guestRepository.save(TestDataBuilder.createGuest());

        ratePlan = ratePlanRepository.save(
                TestDataBuilder.createRatePlan(property.getId(), roomType.getId(),
                                              new BigDecimal("175.00")));
    }

    @Nested
    @DisplayName("Booking Creation")
    class BookingCreation {

        @Test
        @DisplayName("Should create booking with valid data and return confirmation number")
        void createBookingSuccessfully() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));

            // Act & Assert
            MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.confirmationNumber", startsWith("WB-")))
                    .andExpect(jsonPath("$.propertyId").value(property.getId().toString()))
                    .andExpect(jsonPath("$.guestId").value(guest.getId().toString()))
                    .andExpect(jsonPath("$.checkIn").value(request.getCheckIn().toString()))
                    .andExpect(jsonPath("$.checkOut").value(request.getCheckOut().toString()))
                    .andExpect(jsonPath("$.adults").value(2))
                    .andExpect(jsonPath("$.children").value(0))
                    .andReturn();

            // Verify booking was persisted
            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            String confirmationNumber = json.get("confirmationNumber").asText();

            Booking savedBooking = bookingRepository.findByReference(confirmationNumber).orElseThrow();
            assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(savedBooking.getGuestId()).isEqualTo(guest.getId());
        }

        @Test
        @DisplayName("Should create booking for extended stay")
        void createExtendedStayBooking() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(30, 14));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.confirmationNumber", startsWith("WB-")));
        }

        @Test
        @DisplayName("Should create booking with children")
        void createBookingWithChildren() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setAdults(2);
            request.setChildren(2);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.adults").value(2))
                    .andExpect(jsonPath("$.children").value(2));
        }

        @Test
        @DisplayName("Should create multiple bookings for different dates")
        void createMultipleBookings() throws Exception {
            // Arrange
            BookingCreateRequest request1 = buildBookingRequest(DateRange.futureRange(7, 2));
            BookingCreateRequest request2 = buildBookingRequest(DateRange.futureRange(14, 3));

            // Act & Assert - First booking
            MvcResult result1 = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Act & Assert - Second booking
            MvcResult result2 = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Verify both bookings have different confirmation numbers
            String confirmation1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .get("confirmationNumber").asText();
            String confirmation2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .get("confirmationNumber").asText();

            assertThat(confirmation1).isNotEqualTo(confirmation2);
        }
    }

    @Nested
    @DisplayName("Booking Cancellation")
    class BookingCancellation {

        @Test
        @DisplayName("Should cancel booking with valid confirmation number")
        void cancelBookingSuccessfully() throws Exception {
            // Arrange - Create a booking first
            BookingCreateRequest createRequest = buildBookingRequest(DateRange.futureRange(7, 2));
            MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String confirmationNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("confirmationNumber").asText();

            // Act & Assert - Cancel the booking
            mockMvc.perform(post("/api/v1/reservations/" + confirmationNumber + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Guest requested\",\"requestedBy\":\"SYSTEM\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.confirmationNumber").value(confirmationNumber));

            // Verify booking status was updated
            Booking cancelledBooking = bookingRepository.findByReference(confirmationNumber).orElseThrow();
            assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should handle cancellation with different reasons")
        void cancelWithDifferentReasons() throws Exception {
            // Arrange
            BookingCreateRequest createRequest = buildBookingRequest(DateRange.futureRange(7, 2));
            MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String confirmationNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("confirmationNumber").asText();

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations/" + confirmationNumber + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Weather conditions\",\"requestedBy\":\"ADMIN\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return error when cancelling non-existent booking")
        void cancelNonExistentBooking() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations/WB-NONEXISTENT/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Test\",\"requestedBy\":\"SYSTEM\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject cancellation with missing reason")
        void rejectCancellationWithMissingReason() throws Exception {
            // Arrange
            BookingCreateRequest createRequest = buildBookingRequest(DateRange.futureRange(7, 2));
            MvcResult createResult = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String confirmationNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("confirmationNumber").asText();

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations/" + confirmationNumber + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestedBy\":\"SYSTEM\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Complete Booking Lifecycle")
    class BookingLifecycle {

        @Test
        @DisplayName("Should complete full lifecycle: create and cancel")
        void completeCreateAndCancelLifecycle() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));

            // Act - Create booking
            MvcResult creationResult = mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andReturn();

            JsonNode json = objectMapper.readTree(creationResult.getResponse().getContentAsString());
            String confirmationNumber = json.get("confirmationNumber").asText();

            // Assert - Verify creation
            assertThat(confirmationNumber).startsWith("WB-");
            Booking created = bookingRepository.findByReference(confirmationNumber).orElseThrow();
            assertThat(created.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            // Act - Cancel booking
            mockMvc.perform(post("/api/v1/reservations/" + confirmationNumber + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Guest requested\",\"requestedBy\":\"SYSTEM\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            // Assert - Verify cancellation
            Booking cancelled = bookingRepository.findByReference(confirmationNumber).orElseThrow();
            assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should reject booking with missing propertyId")
        void rejectMissingPropertyId() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setPropertyId(null);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with missing guestId")
        void rejectMissingGuestId() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setGuestId(null);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with missing check-in date")
        void rejectMissingCheckIn() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setCheckIn(null);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with missing check-out date")
        void rejectMissingCheckOut() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setCheckOut(null);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with check-out before check-in")
        void rejectCheckOutBeforeCheckIn() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setCheckIn(LocalDate.now().plusDays(10));
            request.setCheckOut(LocalDate.now().plusDays(5));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with past check-in date")
        void rejectPastCheckIn() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setCheckIn(LocalDate.now().minusDays(5));
            request.setCheckOut(LocalDate.now().minusDays(3));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with zero adults")
        void rejectZeroAdults() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setAdults(0);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with negative children")
        void rejectNegativeChildren() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setChildren(-1);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with missing room type")
        void rejectMissingRoomType() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setRoomTypeIds(Set.of());

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with malformed JSON")
        void rejectMalformedJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Business Rule Violations")
    class BusinessRuleViolations {

        @Test
        @DisplayName("Should reject booking for non-existent property")
        void rejectNonExistentProperty() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setPropertyId(UUID.randomUUID());

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject booking for non-existent guest")
        void rejectNonExistentGuest() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setGuestId(UUID.randomUUID());

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject booking for non-existent room type")
        void rejectNonExistentRoomType() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setRoomTypeIds(Set.of(UUID.randomUUID()));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject booking for non-existent rate plan")
        void rejectNonExistentRatePlan() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setRatePlanId(UUID.randomUUID());

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle single night booking")
        void handleSingleNightBooking() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 1));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Should handle booking far in advance")
        void handleFarAdvanceBooking() throws Exception {
            // Arrange - 365 days in advance
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(365, 2));

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Should handle booking with maximum children")
        void handleMaximumChildren() throws Exception {
            // Arrange
            BookingCreateRequest request = buildBookingRequest(DateRange.futureRange(7, 2));
            request.setAdults(2);
            request.setChildren(4);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.children").value(4));
        }
    }

    /**
     * Helper method to build a booking request with default values
     */
    private BookingCreateRequest buildBookingRequest(DateRange dateRange) {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setPropertyId(property.getId());
        request.setGuestId(guest.getId());
        request.setCheckIn(dateRange.start);
        request.setCheckOut(dateRange.end);
        request.setAdults(2);
        request.setChildren(0);
        request.setRatePlanId(ratePlan.getId());
        request.setRoomTypeIds(Set.of(roomType.getId()));
        request.setSource("TEST");
        return request;
    }
}
