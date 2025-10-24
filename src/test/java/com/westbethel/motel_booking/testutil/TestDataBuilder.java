package com.westbethel.motel_booking.testutil;

import com.westbethel.motel_booking.common.model.BookingChannel;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.HousekeepingStatus;
import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import com.westbethel.motel_booking.common.model.RoomStatus;
import com.westbethel.motel_booking.guest.domain.Guest;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.reservation.domain.Booking;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Set;
import java.util.UUID;

/**
 * Test data builder utility providing reusable test fixtures for integration tests.
 * Follows the Builder pattern to create consistent test data across test suites.
 */
public class TestDataBuilder {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_TIMEZONE = "America/New_York";
    private static final String DEFAULT_PROPERTY_CODE = "WB";
    private static final String DEFAULT_PROPERTY_NAME = "West Bethel";

    /**
     * Creates a default Property instance for testing
     */
    public static Property createProperty() {
        return Property.builder()
                .id(UUID.randomUUID())
                .code(DEFAULT_PROPERTY_CODE)
                .name(DEFAULT_PROPERTY_NAME)
                .timezone(ZoneId.of(DEFAULT_TIMEZONE))
                .defaultCurrency(Currency.getInstance(DEFAULT_CURRENCY))
                .build();
    }

    /**
     * Creates a Property with custom parameters
     */
    public static Property createProperty(String code, String name, String timezone) {
        return Property.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .timezone(ZoneId.of(timezone))
                .defaultCurrency(Currency.getInstance(DEFAULT_CURRENCY))
                .build();
    }

    /**
     * Creates a default RoomType for testing
     */
    public static RoomType createRoomType(UUID propertyId) {
        return createRoomType(propertyId, "KING", "King Room", 2, new BigDecimal("150.00"));
    }

    /**
     * Creates a RoomType with custom parameters
     */
    public static RoomType createRoomType(UUID propertyId, String code, String name,
                                         int capacity, BigDecimal baseRate) {
        return RoomType.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .code(code)
                .name(name)
                .capacity(capacity)
                .baseRate(createMoney(baseRate))
                .build();
    }

    /**
     * Creates a Room instance
     */
    public static Room createRoom(UUID propertyId, UUID roomTypeId, String roomNumber) {
        return createRoom(propertyId, roomTypeId, roomNumber, RoomStatus.AVAILABLE,
                         HousekeepingStatus.CLEAN);
    }

    /**
     * Creates a Room with custom status
     */
    public static Room createRoom(UUID propertyId, UUID roomTypeId, String roomNumber,
                                  RoomStatus status, HousekeepingStatus housekeepingStatus) {
        return Room.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .roomTypeId(roomTypeId)
                .roomNumber(roomNumber)
                .status(status)
                .housekeepingStatus(housekeepingStatus)
                .build();
    }

    /**
     * Creates a default Guest instance
     */
    public static Guest createGuest() {
        return createGuest("GUEST-" + UUID.randomUUID().toString().substring(0, 8), true);
    }

    /**
     * Creates a Guest with custom parameters
     */
    public static Guest createGuest(String customerNumber, boolean marketingOptIn) {
        return Guest.builder()
                .id(UUID.randomUUID())
                .customerNumber(customerNumber)
                .marketingOptIn(marketingOptIn)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Creates a default RatePlan
     */
    public static RatePlan createRatePlan(UUID propertyId, UUID roomTypeId, BigDecimal rate) {
        return createRatePlan(propertyId, "Standard", BookingChannel.DIRECT, rate,
                            Set.of(roomTypeId));
    }

    /**
     * Creates a RatePlan with custom parameters
     */
    public static RatePlan createRatePlan(UUID propertyId, String name, BookingChannel channel,
                                         BigDecimal rate, Set<UUID> eligibleRoomTypeIds) {
        return RatePlan.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .name(name)
                .channel(channel)
                .defaultRate(createMoney(rate))
                .eligibleRoomTypeIds(eligibleRoomTypeIds)
                .build();
    }

    /**
     * Creates a default Booking
     */
    public static Booking createBooking(UUID propertyId, UUID guestId, UUID ratePlanId,
                                        LocalDate checkIn, LocalDate checkOut,
                                        BigDecimal totalAmount) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .reference("WB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .guestId(guestId)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.INITIATED)
                .channel(BookingChannel.DIRECT)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .adults(2)
                .children(0)
                .ratePlanId(ratePlanId)
                .roomIds(Set.of())
                .totalAmount(createMoney(totalAmount))
                .balanceDue(createMoney(totalAmount))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Creates a Booking with custom status
     */
    public static Booking createBooking(UUID propertyId, UUID guestId, UUID ratePlanId,
                                        LocalDate checkIn, LocalDate checkOut,
                                        BigDecimal totalAmount, BookingStatus status,
                                        PaymentStatus paymentStatus) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId)
                .reference("WB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .guestId(guestId)
                .status(status)
                .paymentStatus(paymentStatus)
                .channel(BookingChannel.DIRECT)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .adults(2)
                .children(0)
                .ratePlanId(ratePlanId)
                .roomIds(Set.of())
                .totalAmount(createMoney(totalAmount))
                .balanceDue(createMoney(totalAmount))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Creates a Money instance with USD currency
     */
    public static Money createMoney(BigDecimal amount) {
        return Money.builder()
                .amount(amount)
                .currency(Currency.getInstance(DEFAULT_CURRENCY))
                .build();
    }

    /**
     * Creates a Money instance with custom currency
     */
    public static Money createMoney(BigDecimal amount, String currency) {
        return Money.builder()
                .amount(amount)
                .currency(Currency.getInstance(currency))
                .build();
    }

    /**
     * Helper to create date ranges for testing
     */
    public static class DateRange {
        public final LocalDate start;
        public final LocalDate end;

        public DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public static DateRange futureRange(int daysFromNow, int nights) {
            LocalDate start = LocalDate.now().plusDays(daysFromNow);
            return new DateRange(start, start.plusDays(nights));
        }

        public static DateRange pastRange(int daysAgo, int nights) {
            LocalDate start = LocalDate.now().minusDays(daysAgo);
            return new DateRange(start, start.plusDays(nights));
        }

        public int getNights() {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end);
        }
    }
}
