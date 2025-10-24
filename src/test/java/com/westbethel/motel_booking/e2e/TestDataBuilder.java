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
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builder class for creating test data objects with sensible defaults.
 * Provides fluent API for E2E test data setup.
 */
public class TestDataBuilder {

    /**
     * Builder for Property entities
     */
    public static class PropertyBuilder {
        private String name = "West Bethel Motel Test";
        private Address address = AddressBuilder.defaultAddress().build();
        private ContactDetails contactDetails = ContactDetailsBuilder.defaultContact().build();
        private ZoneId timezone = ZoneId.of("America/New_York");

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

        public Property build() {
            Property property = new Property();
            property.setName(name);
            property.setAddress(address);
            property.setContactDetails(contactDetails);
            property.setTimezone(timezone);
            return property;
        }

        public static PropertyBuilder defaultProperty() {
            return new PropertyBuilder();
        }
    }

    /**
     * Builder for Address value objects
     */
    public static class AddressBuilder {
        private String street = "123 Main Street";
        private String city = "West Bethel";
        private String state = "Maine";
        private String postalCode = "04286";
        private String country = "USA";

        public AddressBuilder street(String street) {
            this.street = street;
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
            return new Address(street, city, state, postalCode, country);
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
        private String alternatePhone = null;

        public ContactDetailsBuilder email(String email) {
            this.email = email;
            return this;
        }

        public ContactDetailsBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public ContactDetailsBuilder alternatePhone(String alternatePhone) {
            this.alternatePhone = alternatePhone;
            return this;
        }

        public ContactDetails build() {
            return new ContactDetails(email, phone, alternatePhone);
        }

        public static ContactDetailsBuilder defaultContact() {
            return new ContactDetailsBuilder();
        }
    }

    /**
     * Builder for RoomType entities
     */
    public static class RoomTypeBuilder {
        private String code = "STD";
        private String name = "Standard Room";
        private String description = "Comfortable standard room";
        private Integer maxOccupancy = 2;
        private Integer bedCount = 1;

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

        public RoomTypeBuilder maxOccupancy(Integer maxOccupancy) {
            this.maxOccupancy = maxOccupancy;
            return this;
        }

        public RoomTypeBuilder bedCount(Integer bedCount) {
            this.bedCount = bedCount;
            return this;
        }

        public RoomType build() {
            RoomType roomType = new RoomType();
            roomType.setCode(code);
            roomType.setName(name);
            roomType.setDescription(description);
            roomType.setMaxOccupancy(maxOccupancy);
            roomType.setBedCount(bedCount);
            return roomType;
        }

        public static RoomTypeBuilder defaultRoomType() {
            return new RoomTypeBuilder();
        }

        public static RoomTypeBuilder deluxeRoomType() {
            return new RoomTypeBuilder()
                    .code("DLX")
                    .name("Deluxe Room")
                    .description("Spacious deluxe room")
                    .maxOccupancy(4)
                    .bedCount(2);
        }
    }

    /**
     * Builder for Room entities
     */
    public static class RoomBuilder {
        private String roomNumber = "101";
        private RoomType roomType;
        private Property property;
        private RoomStatus status = RoomStatus.AVAILABLE;

        public RoomBuilder roomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public RoomBuilder roomType(RoomType roomType) {
            this.roomType = roomType;
            return this;
        }

        public RoomBuilder property(Property property) {
            this.property = property;
            return this;
        }

        public RoomBuilder status(RoomStatus status) {
            this.status = status;
            return this;
        }

        public Room build() {
            Room room = new Room();
            room.setRoomNumber(roomNumber);
            room.setRoomType(roomType);
            room.setProperty(property);
            room.setStatus(status);
            return room;
        }

        public static RoomBuilder defaultRoom() {
            return new RoomBuilder();
        }
    }

    /**
     * Builder for RatePlan entities
     */
    public static class RatePlanBuilder {
        private String name = "Standard Rate";
        private String description = "Standard room rate";
        private RoomType roomType;
        private Money baseRate = new Money(new BigDecimal("100.00"), SupportedCurrency.USD);
        private LocalDate validFrom = LocalDate.now().minusDays(30);
        private LocalDate validTo = LocalDate.now().plusDays(365);

        public RatePlanBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RatePlanBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RatePlanBuilder roomType(RoomType roomType) {
            this.roomType = roomType;
            return this;
        }

        public RatePlanBuilder baseRate(Money baseRate) {
            this.baseRate = baseRate;
            return this;
        }

        public RatePlanBuilder validFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public RatePlanBuilder validTo(LocalDate validTo) {
            this.validTo = validTo;
            return this;
        }

        public RatePlan build() {
            RatePlan ratePlan = new RatePlan();
            ratePlan.setName(name);
            ratePlan.setDescription(description);
            ratePlan.setRoomType(roomType);
            ratePlan.setBaseRate(baseRate);
            ratePlan.setValidFrom(validFrom);
            ratePlan.setValidTo(validTo);
            return ratePlan;
        }

        public static RatePlanBuilder defaultRatePlan() {
            return new RatePlanBuilder();
        }
    }

    /**
     * Builder for Guest entities
     */
    public static class GuestBuilder {
        private String firstName = "John";
        private String lastName = "Doe";
        private String email = "john.doe@example.com";
        private String phone = "+1-555-0123";
        private Address address = AddressBuilder.defaultAddress().build();

        public GuestBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public GuestBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public GuestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public GuestBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public GuestBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public Guest build() {
            Guest guest = new Guest();
            guest.setFirstName(firstName);
            guest.setLastName(lastName);
            guest.setEmail(email);
            guest.setPhone(phone);
            guest.setAddress(address);
            return guest;
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
     */
    public static class UserBuilder {
        private String username = "testuser";
        private String email = "testuser@example.com";
        private String password = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"; // "password"
        private String firstName = "Test";
        private String lastName = "User";
        private boolean enabled = true;
        private boolean emailVerified = true;
        private Set<Role> roles = Set.of(Role.CUSTOMER);

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
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(enabled);
            user.setEmailVerified(emailVerified);
            user.setRoles(roles);
            return user;
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
                    .roles(Set.of(Role.ADMIN));
        }

        public static UserBuilder customerUser() {
            return new UserBuilder()
                    .roles(Set.of(Role.CUSTOMER));
        }
    }

    /**
     * Builder for Booking entities
     */
    public static class BookingBuilder {
        private Guest guest;
        private Room room;
        private LocalDate checkInDate = LocalDate.now().plusDays(7);
        private LocalDate checkOutDate = LocalDate.now().plusDays(10);
        private Integer numberOfGuests = 2;
        private BookingStatus status = BookingStatus.CONFIRMED;
        private Money totalAmount = new Money(new BigDecimal("300.00"), SupportedCurrency.USD);
        private String confirmationCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        public BookingBuilder guest(Guest guest) {
            this.guest = guest;
            return this;
        }

        public BookingBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public BookingBuilder checkInDate(LocalDate checkInDate) {
            this.checkInDate = checkInDate;
            return this;
        }

        public BookingBuilder checkOutDate(LocalDate checkOutDate) {
            this.checkOutDate = checkOutDate;
            return this;
        }

        public BookingBuilder numberOfGuests(Integer numberOfGuests) {
            this.numberOfGuests = numberOfGuests;
            return this;
        }

        public BookingBuilder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public BookingBuilder totalAmount(Money totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public BookingBuilder confirmationCode(String confirmationCode) {
            this.confirmationCode = confirmationCode;
            return this;
        }

        public Booking build() {
            Booking booking = new Booking();
            booking.setGuest(guest);
            booking.setRoom(room);
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
            booking.setNumberOfGuests(numberOfGuests);
            booking.setStatus(status);
            booking.setTotalAmount(totalAmount);
            booking.setConfirmationCode(confirmationCode);
            return booking;
        }

        public static BookingBuilder defaultBooking() {
            return new BookingBuilder();
        }
    }
}
