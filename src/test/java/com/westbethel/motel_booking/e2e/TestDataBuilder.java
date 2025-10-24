package com.westbethel.motel_booking.e2e;

import com.westbethel.motel_booking.common.model.*;
import com.westbethel.motel_booking.guest.domain.Guest;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.security.domain.User;
import com.westbethel.motel_booking.security.domain.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builder class for creating test data objects with sensible defaults.
 * Provides fluent API for E2E test data setup.
 * Updated to use entity builder patterns instead of setters.
 */
public class TestDataBuilder {

    /**
     * Builder for Property entities
     */
    public static class PropertyBuilder {
        private UUID id = UUID.randomUUID();
        private String code = "WBM";
        private String name = "West Bethel Motel Test";
        private ZoneId timezone = ZoneId.of("America/New_York");
        private Currency defaultCurrency = Currency.getInstance("USD");
        private Address address = AddressBuilder.defaultAddress().build();
        private ContactDetails contactDetails = ContactDetailsBuilder.defaultContact().build();

        public PropertyBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PropertyBuilder code(String code) {
            this.code = code;
            return this;
        }

        public PropertyBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PropertyBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public PropertyBuilder contactDetails(ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public PropertyBuilder timezone(ZoneId timezone) {
            this.timezone = timezone;
            return this;
        }

        public PropertyBuilder defaultCurrency(Currency defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
            return this;
        }

        public Property build() {
            return Property.builder()
                    .id(id)
                    .code(code)
                    .name(name)
                    .timezone(timezone)
                    .defaultCurrency(defaultCurrency)
                    .address(address)
                    .contactDetails(contactDetails)
                    .build();
        }

        public static PropertyBuilder defaultProperty() {
            return new PropertyBuilder();
        }
    }

    /**
     * Builder for Address value objects
     */
    public static class AddressBuilder {
        private String line1 = "123 Main Street";
        private String line2 = null;
        private String city = "West Bethel";
        private String state = "Maine";
        private String postalCode = "04286";
        private String country = "USA";

        public AddressBuilder line1(String line1) {
            this.line1 = line1;
            return this;
        }

        public AddressBuilder line2(String line2) {
            this.line2 = line2;
            return this;
        }

        public AddressBuilder city(String city) {
            this.city = city;
            return this;
        }

        public AddressBuilder state(String state) {
            this.state = state;
            return this;
        }

        public AddressBuilder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public AddressBuilder country(String country) {
            this.country = country;
            return this;
        }

        public Address build() {
            return Address.builder()
                    .line1(line1)
                    .line2(line2)
                    .city(city)
                    .state(state)
                    .postalCode(postalCode)
                    .country(country)
                    .build();
        }

        public static AddressBuilder defaultAddress() {
            return new AddressBuilder();
        }
    }

    /**
     * Builder for ContactDetails value objects
     */
    public static class ContactDetailsBuilder {
        private String email = "info@westbethelmotel.com";
        private String phone = "+1-207-555-0100";

        public ContactDetailsBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ContactDetailsBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public ContactDetails build() {
            return ContactDetails.builder()
                    .email(email)
                    .phone(phone)
                    .build();
        }

        public static ContactDetailsBuilder defaultContact() {
            return new ContactDetailsBuilder();
        }
    }

    /**
     * Builder for RoomType entities
     */
    public static class RoomTypeBuilder {
        private UUID id = UUID.randomUUID();
        private UUID propertyId = UUID.randomUUID();
        private String code = "STD";
        private String name = "Standard Room";
        private String description = "Comfortable standard room";
        private Integer capacity = 2;
        private String bedConfiguration = "1 Queen Bed";
        private Set<String> amenities = Set.of("WiFi", "TV", "Air Conditioning");
        private Money baseRate = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));

        public RoomTypeBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RoomTypeBuilder propertyId(UUID propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public RoomTypeBuilder code(String code) {
            this.code = code;
            return this;
        }

        public RoomTypeBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RoomTypeBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RoomTypeBuilder capacity(Integer capacity) {
            this.capacity = capacity;
            return this;
        }

        public RoomTypeBuilder bedConfiguration(String bedConfiguration) {
            this.bedConfiguration = bedConfiguration;
            return this;
        }

        public RoomTypeBuilder amenities(Set<String> amenities) {
            this.amenities = amenities;
            return this;
        }

        public RoomTypeBuilder baseRate(Money baseRate) {
            this.baseRate = baseRate;
            return this;
        }

        public RoomType build() {
            return RoomType.builder()
                    .id(id)
                    .propertyId(propertyId)
                    .code(code)
                    .name(name)
                    .description(description)
                    .capacity(capacity)
                    .bedConfiguration(bedConfiguration)
                    .amenities(amenities)
                    .baseRate(baseRate)
                    .build();
        }

        public static RoomTypeBuilder defaultRoomType() {
            return new RoomTypeBuilder();
        }

        public static RoomTypeBuilder deluxeRoomType() {
            return new RoomTypeBuilder()
                    .code("DLX")
                    .name("Deluxe Room")
                    .description("Spacious deluxe room")
                    .capacity(4)
                    .bedConfiguration("2 Queen Beds");
        }
    }

    /**
     * Builder for Room entities
     */
    public static class RoomBuilder {
        private UUID id = UUID.randomUUID();
        private UUID propertyId = UUID.randomUUID();
        private UUID roomTypeId = UUID.randomUUID();
        private String roomNumber = "101";
        private String floor = "1";
        private RoomStatus status = RoomStatus.AVAILABLE;
        private HousekeepingStatus housekeepingStatus = HousekeepingStatus.CLEAN;
        private String maintenanceNotes = null;

        public RoomBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RoomBuilder propertyId(UUID propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public RoomBuilder roomTypeId(UUID roomTypeId) {
            this.roomTypeId = roomTypeId;
            return this;
        }

        public RoomBuilder roomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public RoomBuilder floor(String floor) {
            this.floor = floor;
            return this;
        }

        public RoomBuilder status(RoomStatus status) {
            this.status = status;
            return this;
        }

        public RoomBuilder housekeepingStatus(HousekeepingStatus housekeepingStatus) {
            this.housekeepingStatus = housekeepingStatus;
            return this;
        }

        public RoomBuilder maintenanceNotes(String maintenanceNotes) {
            this.maintenanceNotes = maintenanceNotes;
            return this;
        }

        public Room build() {
            return Room.builder()
                    .id(id)
                    .propertyId(propertyId)
                    .roomTypeId(roomTypeId)
                    .roomNumber(roomNumber)
                    .floor(floor)
                    .status(status)
                    .housekeepingStatus(housekeepingStatus)
                    .maintenanceNotes(maintenanceNotes)
                    .build();
        }

        public static RoomBuilder defaultRoom() {
            return new RoomBuilder();
        }
    }

    /**
     * Builder for RatePlan entities
     */
    public static class RatePlanBuilder {
        private UUID id = UUID.randomUUID();
        private UUID propertyId = UUID.randomUUID();
        private UUID roomTypeId = UUID.randomUUID();
        private String code = "STD";
        private String name = "Standard Rate";
        private String description = "Standard room rate";
        private Money baseRate = new Money(new BigDecimal("100.00"), Currency.getInstance("USD"));
        private LocalDate effectiveFrom = LocalDate.now().minusDays(30);
        private LocalDate effectiveTo = LocalDate.now().plusDays(365);

        public RatePlanBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RatePlanBuilder propertyId(UUID propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public RatePlanBuilder roomTypeId(UUID roomTypeId) {
            this.roomTypeId = roomTypeId;
            return this;
        }

        public RatePlanBuilder code(String code) {
            this.code = code;
            return this;
        }

        public RatePlanBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RatePlanBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RatePlanBuilder baseRate(Money baseRate) {
            this.baseRate = baseRate;
            return this;
        }

        public RatePlanBuilder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public RatePlanBuilder effectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public RatePlan build() {
            return RatePlan.builder()
                    .id(id)
                    .propertyId(propertyId)
                    .name(name)
                    .build();
        }

        public static RatePlanBuilder defaultRatePlan() {
            return new RatePlanBuilder();
        }
    }

    /**
     * Builder for Guest entities
     */
    public static class GuestBuilder {
        private UUID id = UUID.randomUUID();
        private String customerNumber = "CUST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private ContactDetails contactDetails = ContactDetails.builder()
                .email("john.doe@example.com")
                .phone("+1-555-0123")
                .build();
        private Address address = AddressBuilder.defaultAddress().build();
        private String preferences = null;
        private Boolean marketingOptIn = false;
        private UUID loyaltyProfileId = null;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime updatedAt = null;

        public GuestBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public GuestBuilder customerNumber(String customerNumber) {
            this.customerNumber = customerNumber;
            return this;
        }

        public GuestBuilder email(String email) {
            this.contactDetails = ContactDetails.builder()
                    .email(email)
                    .phone(this.contactDetails.getPhone())
                    .build();
            return this;
        }

        public GuestBuilder phone(String phone) {
            this.contactDetails = ContactDetails.builder()
                    .email(this.contactDetails.getEmail())
                    .phone(phone)
                    .build();
            return this;
        }

        public GuestBuilder contactDetails(ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public GuestBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public GuestBuilder preferences(String preferences) {
            this.preferences = preferences;
            return this;
        }

        public GuestBuilder marketingOptIn(Boolean marketingOptIn) {
            this.marketingOptIn = marketingOptIn;
            return this;
        }

        public GuestBuilder loyaltyProfileId(UUID loyaltyProfileId) {
            this.loyaltyProfileId = loyaltyProfileId;
            return this;
        }

        public Guest build() {
            return Guest.builder()
                    .id(id)
                    .customerNumber(customerNumber)
                    .contactDetails(contactDetails)
                    .address(address)
                    .preferences(preferences)
                    .marketingOptIn(marketingOptIn)
                    .loyaltyProfileId(loyaltyProfileId)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }

        public static GuestBuilder defaultGuest() {
            return new GuestBuilder();
        }

        public static GuestBuilder withEmail(String email) {
            return new GuestBuilder().email(email);
        }
    }

    /**
     * Builder for User entities
     * Note: User entity has @Setter, so we can use setters here
     */
    public static class UserBuilder {
        private String username = "testuser";
        private String email = "testuser@example.com";
        private String password = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"; // "password"
        private String firstName = "Test";
        private String lastName = "User";
        private boolean enabled = true;
        private boolean emailVerified = true;
        private Set<Role> roles = Set.of(createCustomerRole());

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserBuilder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            return User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(password)
                    .firstName(firstName)
                    .lastName(lastName)
                    .enabled(enabled)
                    .build();
        }

        public static UserBuilder defaultUser() {
            return new UserBuilder();
        }

        public static UserBuilder adminUser() {
            return new UserBuilder()
                    .username("admin")
                    .email("admin@westbethelmotel.com")
                    .firstName("Admin")
                    .lastName("User")
                    .roles(Set.of(createAdminRole()));
        }

        public static UserBuilder customerUser() {
            return new UserBuilder()
                    .roles(Set.of(createCustomerRole()));
        }

        private static Role createCustomerRole() {
            return Role.builder()
                    .name("ROLE_CUSTOMER")
                    .description("Customer role")
                    .build();
        }

        private static Role createAdminRole() {
            return Role.builder()
                    .name("ROLE_ADMIN")
                    .description("Admin role")
                    .build();
        }
    }

    /**
     * Builder for Booking entities
     */
    public static class BookingBuilder {
        private UUID id = UUID.randomUUID();
        private UUID propertyId = UUID.randomUUID();
        private String reference = "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        private UUID guestId = UUID.randomUUID();
        private BookingStatus status = BookingStatus.CONFIRMED;
        private PaymentStatus paymentStatus = PaymentStatus.INITIATED;
        private BookingChannel channel = BookingChannel.DIRECT;
        private String source = "web";
        private String createdBy = "system";
        private LocalDate checkIn = LocalDate.now().plusDays(7);
        private LocalDate checkOut = LocalDate.now().plusDays(10);
        private Integer adults = 2;
        private Integer children = 0;
        private UUID ratePlanId = UUID.randomUUID();
        private Set<UUID> roomIds = Set.of(UUID.randomUUID());
        private Money totalAmount = new Money(new BigDecimal("300.00"), Currency.getInstance("USD"));
        private Money balanceDue = new Money(new BigDecimal("300.00"), Currency.getInstance("USD"));
        private String notes = null;
        private OffsetDateTime createdAt = OffsetDateTime.now();
        private OffsetDateTime updatedAt = null;

        public BookingBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public BookingBuilder propertyId(UUID propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public BookingBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public BookingBuilder guestId(UUID guestId) {
            this.guestId = guestId;
            return this;
        }

        public BookingBuilder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public BookingBuilder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public BookingBuilder channel(BookingChannel channel) {
            this.channel = channel;
            return this;
        }

        public BookingBuilder checkIn(LocalDate checkIn) {
            this.checkIn = checkIn;
            return this;
        }

        public BookingBuilder checkOut(LocalDate checkOut) {
            this.checkOut = checkOut;
            return this;
        }

        public BookingBuilder adults(Integer adults) {
            this.adults = adults;
            return this;
        }

        public BookingBuilder children(Integer children) {
            this.children = children;
            return this;
        }

        public BookingBuilder ratePlanId(UUID ratePlanId) {
            this.ratePlanId = ratePlanId;
            return this;
        }

        public BookingBuilder roomIds(Set<UUID> roomIds) {
            this.roomIds = roomIds;
            return this;
        }

        public BookingBuilder totalAmount(Money totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public BookingBuilder balanceDue(Money balanceDue) {
            this.balanceDue = balanceDue;
            return this;
        }

        public BookingBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Booking build() {
            return Booking.builder()
                    .id(id)
                    .propertyId(propertyId)
                    .reference(reference)
                    .guestId(guestId)
                    .status(status)
                    .paymentStatus(paymentStatus)
                    .channel(channel)
                    .source(source)
                    .createdBy(createdBy)
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .adults(adults)
                    .children(children)
                    .ratePlanId(ratePlanId)
                    .roomIds(roomIds)
                    .totalAmount(totalAmount)
                    .balanceDue(balanceDue)
                    .notes(notes)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }

        public static BookingBuilder defaultBooking() {
            return new BookingBuilder();
        }
    }
}
