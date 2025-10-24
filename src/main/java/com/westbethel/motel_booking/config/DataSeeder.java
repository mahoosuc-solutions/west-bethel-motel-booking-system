package com.westbethel.motel_booking.config;

import com.westbethel.motel_booking.common.model.Address;
import com.westbethel.motel_booking.common.model.BookingChannel;
import com.westbethel.motel_booking.common.model.ContactDetails;
import com.westbethel.motel_booking.common.model.HousekeepingStatus;
import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.RoomStatus;
import com.westbethel.motel_booking.guest.domain.Guest;
import com.westbethel.motel_booking.guest.repository.GuestRepository;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.inventory.repository.RoomRepository;
import com.westbethel.motel_booking.inventory.repository.RoomTypeRepository;
import com.westbethel.motel_booking.loyalty.domain.LoyaltyProfile;
import com.westbethel.motel_booking.loyalty.domain.LoyaltyTier;
import com.westbethel.motel_booking.loyalty.repository.LoyaltyProfileRepository;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.pricing.repository.RatePlanRepository;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Database seeder for development and testing environments.
 * Seeds the database with comprehensive test data for the West Bethel Motel.
 *
 * To enable: Run with profile 'dev' (e.g., --spring.profiles.active=dev)
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final RatePlanRepository ratePlanRepository;
    private final LoyaltyProfileRepository loyaltyProfileRepository;

    // Constants for consistent UUIDs across seeding
    private static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STANDARD_ROOM_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID DELUXE_ROOM_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID SUITE_ROOM_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");

    private static final UUID GUEST_JOHN_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID GUEST_JANE_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID GUEST_BOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");

    private static final UUID RATE_PLAN_STANDARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID RATE_PLAN_WEEKEND_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");

    private static final UUID LOYALTY_JANE_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

    private static final Currency USD = Currency.getInstance("USD");

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("Starting Database Seeding for Development");
        log.info("========================================");

        // Check if data already exists
        if (propertyRepository.findByCode("WBM").isPresent()) {
            log.info("Data already seeded. Skipping...");
            return;
        }

        try {
            seedProperty();
            seedRoomTypes();
            seedRooms();
            seedGuests();
            seedLoyaltyProfiles();
            seedRatePlans();

            log.info("========================================");
            log.info("Database Seeding Completed Successfully");
            log.info("========================================");
            printSeedingSummary();
        } catch (Exception e) {
            log.error("Error during database seeding", e);
            throw new RuntimeException("Failed to seed database", e);
        }
    }

    private void seedProperty() {
        log.info("Seeding Property...");

        Property property = Property.builder()
                .id(PROPERTY_ID)
                .code("WBM")
                .name("West Bethel Motel")
                .timezone(ZoneId.of("America/New_York"))
                .defaultCurrency(USD)
                .address(Address.builder()
                        .line1("123 Mountain View Road")
                        .line2(null)
                        .city("West Bethel")
                        .state("ME")
                        .postalCode("04286")
                        .country("USA")
                        .build())
                .contactDetails(ContactDetails.builder()
                        .email("info@westbethelmotel.com")
                        .phone("+1-207-555-0100")
                        .build())
                .build();

        propertyRepository.save(property);
        log.info("  - Seeded property: {} ({})", property.getName(), property.getCode());
    }

    private void seedRoomTypes() {
        log.info("Seeding Room Types...");

        // Standard Room
        Set<String> standardAmenities = new HashSet<>();
        standardAmenities.add("WiFi");
        standardAmenities.add("TV");
        standardAmenities.add("Coffee Maker");
        standardAmenities.add("Mini Fridge");

        RoomType standardRoom = RoomType.builder()
                .id(STANDARD_ROOM_TYPE_ID)
                .propertyId(PROPERTY_ID)
                .code("STANDARD")
                .name("Standard Room")
                .description("Comfortable standard room with queen bed and essential amenities")
                .capacity(2)
                .bedConfiguration("1 Queen Bed")
                .amenities(standardAmenities)
                .baseRate(Money.builder()
                        .amount(new BigDecimal("89.00"))
                        .currency(USD)
                        .build())
                .build();

        // Deluxe Room
        Set<String> deluxeAmenities = new HashSet<>();
        deluxeAmenities.add("WiFi");
        deluxeAmenities.add("Smart TV");
        deluxeAmenities.add("Coffee Maker");
        deluxeAmenities.add("Mini Fridge");
        deluxeAmenities.add("Microwave");
        deluxeAmenities.add("Work Desk");

        RoomType deluxeRoom = RoomType.builder()
                .id(DELUXE_ROOM_TYPE_ID)
                .propertyId(PROPERTY_ID)
                .code("DELUXE")
                .name("Deluxe Room")
                .description("Spacious deluxe room with king bed and premium amenities")
                .capacity(3)
                .bedConfiguration("1 King Bed")
                .amenities(deluxeAmenities)
                .baseRate(Money.builder()
                        .amount(new BigDecimal("129.00"))
                        .currency(USD)
                        .build())
                .build();

        // Executive Suite
        Set<String> suiteAmenities = new HashSet<>();
        suiteAmenities.add("WiFi");
        suiteAmenities.add("Smart TV");
        suiteAmenities.add("Coffee Maker");
        suiteAmenities.add("Full Refrigerator");
        suiteAmenities.add("Microwave");
        suiteAmenities.add("Executive Work Desk");
        suiteAmenities.add("Separate Living Area");
        suiteAmenities.add("Whirlpool Tub");

        RoomType executiveSuite = RoomType.builder()
                .id(SUITE_ROOM_TYPE_ID)
                .propertyId(PROPERTY_ID)
                .code("SUITE")
                .name("Executive Suite")
                .description("Luxurious executive suite with separate living area, king bed, and sofa bed")
                .capacity(4)
                .bedConfiguration("1 King Bed + 1 Sofa Bed")
                .amenities(suiteAmenities)
                .baseRate(Money.builder()
                        .amount(new BigDecimal("199.00"))
                        .currency(USD)
                        .build())
                .build();

        roomTypeRepository.save(standardRoom);
        roomTypeRepository.save(deluxeRoom);
        roomTypeRepository.save(executiveSuite);

        log.info("  - Seeded 3 room types: STANDARD, DELUXE, SUITE");
    }

    private void seedRooms() {
        log.info("Seeding Rooms...");

        int roomCount = 0;

        // 5 Standard Rooms (101-105)
        for (int i = 1; i <= 5; i++) {
            String roomNumber = "10" + i;
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .propertyId(PROPERTY_ID)
                    .roomTypeId(STANDARD_ROOM_TYPE_ID)
                    .roomNumber(roomNumber)
                    .floor("1")
                    .status(RoomStatus.AVAILABLE)
                    .housekeepingStatus(HousekeepingStatus.CLEAN)
                    .maintenanceNotes(null)
                    .build();
            roomRepository.save(room);
            roomCount++;
        }

        // 3 Deluxe Rooms (201-203)
        for (int i = 1; i <= 3; i++) {
            String roomNumber = "20" + i;
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .propertyId(PROPERTY_ID)
                    .roomTypeId(DELUXE_ROOM_TYPE_ID)
                    .roomNumber(roomNumber)
                    .floor("2")
                    .status(RoomStatus.AVAILABLE)
                    .housekeepingStatus(HousekeepingStatus.CLEAN)
                    .maintenanceNotes(null)
                    .build();
            roomRepository.save(room);
            roomCount++;
        }

        // 2 Executive Suites (301-302)
        for (int i = 1; i <= 2; i++) {
            String roomNumber = "30" + i;
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .propertyId(PROPERTY_ID)
                    .roomTypeId(SUITE_ROOM_TYPE_ID)
                    .roomNumber(roomNumber)
                    .floor("3")
                    .status(RoomStatus.AVAILABLE)
                    .housekeepingStatus(HousekeepingStatus.CLEAN)
                    .maintenanceNotes(null)
                    .build();
            roomRepository.save(room);
            roomCount++;
        }

        log.info("  - Seeded {} rooms: 5 STANDARD, 3 DELUXE, 2 SUITE", roomCount);
    }

    private void seedGuests() {
        log.info("Seeding Guests...");

        OffsetDateTime now = OffsetDateTime.now();

        // Guest 1: John Doe
        Guest johnDoe = Guest.builder()
                .id(GUEST_JOHN_ID)
                .customerNumber("CUST-001")
                .contactDetails(ContactDetails.builder()
                        .email("john.doe@example.com")
                        .phone("+1-555-100-0001")
                        .build())
                .address(Address.builder()
                        .line1("456 Oak Street")
                        .line2("Apt 2B")
                        .city("Portland")
                        .state("ME")
                        .postalCode("04101")
                        .country("USA")
                        .build())
                .preferences("Non-smoking, Ground floor preferred")
                .marketingOptIn(true)
                .loyaltyProfileId(null)
                .createdAt(now.minusMonths(6))
                .updatedAt(now.minusMonths(1))
                .build();

        // Guest 2: Jane Smith (with loyalty profile)
        Guest janeSmith = Guest.builder()
                .id(GUEST_JANE_ID)
                .customerNumber("CUST-002")
                .contactDetails(ContactDetails.builder()
                        .email("jane.smith@example.com")
                        .phone("+1-555-200-0002")
                        .build())
                .address(Address.builder()
                        .line1("789 Maple Avenue")
                        .line2(null)
                        .city("Bethel")
                        .state("ME")
                        .postalCode("04217")
                        .country("USA")
                        .build())
                .preferences("Quiet room, High floor, Extra pillows")
                .marketingOptIn(true)
                .loyaltyProfileId(LOYALTY_JANE_ID)
                .createdAt(now.minusYears(2))
                .updatedAt(now.minusWeeks(2))
                .build();

        // Guest 3: Bob Jones
        Guest bobJones = Guest.builder()
                .id(GUEST_BOB_ID)
                .customerNumber("CUST-003")
                .contactDetails(ContactDetails.builder()
                        .email("bob.jones@example.com")
                        .phone("+1-555-300-0003")
                        .build())
                .address(Address.builder()
                        .line1("321 Pine Road")
                        .line2("Suite 100")
                        .city("Augusta")
                        .state("ME")
                        .postalCode("04330")
                        .country("USA")
                        .build())
                .preferences("King bed, Late checkout if available")
                .marketingOptIn(false)
                .loyaltyProfileId(null)
                .createdAt(now.minusMonths(3))
                .updatedAt(now.minusDays(5))
                .build();

        guestRepository.save(johnDoe);
        guestRepository.save(janeSmith);
        guestRepository.save(bobJones);

        log.info("  - Seeded 3 guests: john.doe@example.com, jane.smith@example.com, bob.jones@example.com");
    }

    private void seedLoyaltyProfiles() {
        log.info("Seeding Loyalty Profiles...");

        OffsetDateTime now = OffsetDateTime.now();

        // Loyalty profile for Jane Smith
        LoyaltyProfile janeProfile = LoyaltyProfile.builder()
                .id(LOYALTY_JANE_ID)
                .guestId(GUEST_JANE_ID)
                .tier(LoyaltyTier.GOLD)
                .pointsBalance(2500L)
                .pointsExpiryPolicy("Points expire after 24 months of inactivity")
                .updatedAt(now.minusWeeks(1))
                .build();

        loyaltyProfileRepository.save(janeProfile);

        log.info("  - Seeded 1 loyalty profile: Jane Smith (GOLD tier, 2500 points)");
    }

    private void seedRatePlans() {
        log.info("Seeding Rate Plans...");

        // Get all room type IDs for eligibility
        Set<UUID> allRoomTypeIds = new HashSet<>();
        allRoomTypeIds.add(STANDARD_ROOM_TYPE_ID);
        allRoomTypeIds.add(DELUXE_ROOM_TYPE_ID);
        allRoomTypeIds.add(SUITE_ROOM_TYPE_ID);

        // Standard Rate Plan
        RatePlan standardRate = RatePlan.builder()
                .id(RATE_PLAN_STANDARD_ID)
                .propertyId(PROPERTY_ID)
                .name("Standard Rate")
                .channel(BookingChannel.DIRECT)
                .eligibleRoomTypeIds(allRoomTypeIds)
                .defaultRate(Money.builder()
                        .amount(new BigDecimal("0.00"))
                        .currency(USD)
                        .build())
                .pricingRules("Standard pricing applies room type base rates")
                .cancellationPolicy("Free cancellation up to 24 hours before check-in. " +
                        "Cancellations within 24 hours incur a one-night charge.")
                .stayRestrictions("Minimum stay: 1 night. Maximum stay: 30 nights.")
                .build();

        // Weekend Special Rate Plan
        Set<UUID> weekendRoomTypeIds = new HashSet<>();
        weekendRoomTypeIds.add(STANDARD_ROOM_TYPE_ID);
        weekendRoomTypeIds.add(DELUXE_ROOM_TYPE_ID);

        RatePlan weekendSpecial = RatePlan.builder()
                .id(RATE_PLAN_WEEKEND_ID)
                .propertyId(PROPERTY_ID)
                .name("Weekend Special")
                .channel(BookingChannel.DIRECT)
                .eligibleRoomTypeIds(weekendRoomTypeIds)
                .defaultRate(Money.builder()
                        .amount(new BigDecimal("0.00"))
                        .currency(USD)
                        .build())
                .pricingRules("10% discount on base rates for Friday and Saturday nights. " +
                        "15% discount for stays of 2+ weekend nights.")
                .cancellationPolicy("Free cancellation up to 48 hours before check-in. " +
                        "Cancellations within 48 hours incur a 50% charge.")
                .stayRestrictions("Minimum stay: 2 nights (must include Friday or Saturday). " +
                        "Maximum stay: 7 nights.")
                .build();

        ratePlanRepository.save(standardRate);
        ratePlanRepository.save(weekendSpecial);

        log.info("  - Seeded 2 rate plans: Standard Rate, Weekend Special");
    }

    private void printSeedingSummary() {
        log.info("");
        log.info("Seeding Summary:");
        log.info("  Properties: {}", propertyRepository.count());
        log.info("  Room Types: {}", roomTypeRepository.count());
        log.info("  Rooms: {}", roomRepository.count());
        log.info("  Guests: {}", guestRepository.count());
        log.info("  Loyalty Profiles: {}", loyaltyProfileRepository.count());
        log.info("  Rate Plans: {}", ratePlanRepository.count());
        log.info("");
        log.info("Test Data Details:");
        log.info("  Property Code: WBM");
        log.info("  Timezone: America/New_York");
        log.info("  Currency: USD");
        log.info("");
        log.info("  Room Types:");
        log.info("    - STANDARD: $89/night (2 guests, Queen bed) - 5 rooms (101-105)");
        log.info("    - DELUXE: $129/night (3 guests, King bed) - 3 rooms (201-203)");
        log.info("    - SUITE: $199/night (4 guests, King + Sofa bed) - 2 rooms (301-302)");
        log.info("");
        log.info("  Test Guests:");
        log.info("    - john.doe@example.com (Regular guest)");
        log.info("    - jane.smith@example.com (GOLD loyalty member with 2500 points)");
        log.info("    - bob.jones@example.com (Regular guest)");
        log.info("");
        log.info("  Rate Plans:");
        log.info("    - Standard Rate (DIRECT channel, all room types)");
        log.info("    - Weekend Special (DIRECT channel, STANDARD & DELUXE only)");
        log.info("");
    }
}
