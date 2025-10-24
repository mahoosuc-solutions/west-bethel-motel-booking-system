package com.westbethel.motel_booking.security;

import com.westbethel.motel_booking.billing.api.dto.PaymentAmountDto;
import com.westbethel.motel_booking.common.model.SupportedCurrency;
import com.westbethel.motel_booking.common.validation.*;
import com.westbethel.motel_booking.guest.api.dto.GuestCreateRequest;
import com.westbethel.motel_booking.reservation.api.dto.BookingCreateRequest;
import com.westbethel.motel_booking.security.util.InjectionPayloadProvider;
import jakarta.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive input validation tests for all DTOs.
 * Tests cover: SQL injection, XSS, path traversal, command injection,
 * invalid formats, boundary values, null handling, and oversized input.
 */
class InputValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== Currency Validation Tests ====================

    @Test
    void testValidCurrencyCodes() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("100.00"));

        // Test all supported currencies
        for (SupportedCurrency currency : SupportedCurrency.values()) {
            dto.setCurrency(currency.getCode());
            Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getInvalidCurrencyPayloads")
    void testInvalidCurrencyCodesRejected(String invalidCurrency) {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setCurrency(invalidCurrency);

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testCurrencySqlInjection() {
        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            PaymentAmountDto dto = new PaymentAmountDto();
            dto.setAmount(new BigDecimal("100.00"));
            dto.setCurrency(payload);

            Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Test
    void testNullCurrencyRejected() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setCurrency(null);

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testBlankCurrencyRejected() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setCurrency("");

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    // ==================== Amount Validation Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1.00", "999999.99"})
    void testValidAmounts(String amount) {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal(amount));
        dto.setCurrency("USD");

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider.NumericBoundaries#getDecimalBoundaries")
    void testAmountBoundaries(String amount) {
        PaymentAmountDto dto = new PaymentAmountDto();
        try {
            dto.setAmount(new BigDecimal(amount));
            dto.setCurrency("USD");

            Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);

            // Valid amounts: 0.01 to 999999.99
            BigDecimal value = new BigDecimal(amount);
            if (value.compareTo(new BigDecimal("0.01")) >= 0 &&
                value.compareTo(new BigDecimal("999999.99")) <= 0 &&
                value.scale() <= 2) {
                assertThat(violations).isEmpty();
            } else {
                assertThat(violations).isNotEmpty();
            }
        } catch (NumberFormatException e) {
            // Expected for invalid numbers
        }
    }

    @Test
    void testNullAmountRejected() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(null);
        dto.setCurrency("USD");

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testNegativeAmountRejected() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("-10.00"));
        dto.setCurrency("USD");

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testZeroAmountRejected() {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(new BigDecimal("0.00"));
        dto.setCurrency("USD");

        Set<ConstraintViolation<PaymentAmountDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    // ==================== Email Validation Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "test.user@example.com",
        "user+tag@example.co.uk",
        "user_name@example.com"
    })
    void testValidEmails(String email) {
        GuestCreateRequest.ContactDetailsDto contact = new GuestCreateRequest.ContactDetailsDto();
        contact.setEmail(email);
        contact.setPhone("+1-234-567-8900");

        Set<ConstraintViolation<GuestCreateRequest.ContactDetailsDto>> violations = validator.validate(contact);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getInvalidEmailPayloads")
    void testInvalidEmailsRejected(String email) {
        GuestCreateRequest.ContactDetailsDto contact = new GuestCreateRequest.ContactDetailsDto();
        contact.setEmail(email);
        contact.setPhone("+1-234-567-8900");

        Set<ConstraintViolation<GuestCreateRequest.ContactDetailsDto>> violations = validator.validate(contact);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testEmailSqlInjection() {
        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            GuestCreateRequest.ContactDetailsDto contact = new GuestCreateRequest.ContactDetailsDto();
            contact.setEmail(payload + "@example.com");
            contact.setPhone("+1-234-567-8900");

            Set<ConstraintViolation<GuestCreateRequest.ContactDetailsDto>> violations = validator.validate(contact);
            assertThat(violations).isNotEmpty();
        }
    }

    @Test
    void testEmailXssInjection() {
        for (String payload : InjectionPayloadProvider.getXssPayloads()) {
            GuestCreateRequest.ContactDetailsDto contact = new GuestCreateRequest.ContactDetailsDto();
            contact.setEmail(payload);
            contact.setPhone("+1-234-567-8900");

            Set<ConstraintViolation<GuestCreateRequest.ContactDetailsDto>> violations = validator.validate(contact);
            assertThat(violations).isNotEmpty();
        }
    }

    @Test
    void testOversizedEmailRejected() {
        String longEmail = "a".repeat(250) + "@example.com";
        GuestCreateRequest.ContactDetailsDto contact = new GuestCreateRequest.ContactDetailsDto();
        contact.setEmail(longEmail);
        contact.setPhone("+1-234-567-8900");

        Set<ConstraintViolation<GuestCreateRequest.ContactDetailsDto>> violations = validator.validate(contact);
        assertThat(violations).isNotEmpty();
    }

    // ==================== Phone Number Validation Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {
        "+1-234-567-8900",
        "+44 20 7946 0958",
        "+33 1 42 86 82 00",
        "+1 (234) 567-8900"
    })
    void testValidPhoneNumbers(String phone) {
        PhoneNumberValidator validator = new PhoneNumberValidator();
        ValidPhoneNumber annotation = createPhoneAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid(phone, null)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getInvalidPhonePayloads")
    void testInvalidPhoneNumbersRejected(String phone) {
        PhoneNumberValidator validator = new PhoneNumberValidator();
        ValidPhoneNumber annotation = createPhoneAnnotation(true);
        validator.initialize(annotation);

        assertThat(validator.isValid(phone, null)).isFalse();
    }

    @Test
    void testPhoneSqlInjection() {
        PhoneNumberValidator validator = new PhoneNumberValidator();
        ValidPhoneNumber annotation = createPhoneAnnotation(true);
        validator.initialize(annotation);

        for (String payload : InjectionPayloadProvider.getSqlInjectionPayloads()) {
            assertThat(validator.isValid(payload, null)).isFalse();
        }
    }

    // ==================== UUID Validation Tests ====================

    @Test
    void testValidUuid() {
        UUIDValidator validator = new UUIDValidator();
        String validUuid = UUID.randomUUID().toString();

        assertThat(validator.isValid(validUuid, null)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("com.westbethel.motel_booking.security.util.InjectionPayloadProvider#getInvalidUuidPayloads")
    void testInvalidUuidsRejected(String uuid) {
        UUIDValidator validator = new UUIDValidator();
        assertThat(validator.isValid(uuid, null)).isFalse();
    }

    // ==================== Date Range Validation Tests ====================

    @Test
    void testValidDateRange() {
        BookingCreateRequest booking = new BookingCreateRequest();
        booking.setPropertyId(UUID.randomUUID());
        booking.setGuestId(UUID.randomUUID());
        booking.setCheckIn(LocalDate.now().plusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(5));
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setRatePlanId(UUID.randomUUID());
        booking.setRoomTypeIds(Set.of(UUID.randomUUID()));

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isEmpty();
    }

    @Test
    void testCheckOutBeforeCheckInRejected() {
        BookingCreateRequest booking = new BookingCreateRequest();
        booking.setPropertyId(UUID.randomUUID());
        booking.setGuestId(UUID.randomUUID());
        booking.setCheckIn(LocalDate.now().plusDays(5));
        booking.setCheckOut(LocalDate.now().plusDays(1));
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setRatePlanId(UUID.randomUUID());
        booking.setRoomTypeIds(Set.of(UUID.randomUUID()));

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testSameDayCheckInCheckOutRejected() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        BookingCreateRequest booking = new BookingCreateRequest();
        booking.setPropertyId(UUID.randomUUID());
        booking.setGuestId(UUID.randomUUID());
        booking.setCheckIn(tomorrow);
        booking.setCheckOut(tomorrow);
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setRatePlanId(UUID.randomUUID());
        booking.setRoomTypeIds(Set.of(UUID.randomUUID()));

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testExcessiveStayDurationRejected() {
        BookingCreateRequest booking = new BookingCreateRequest();
        booking.setPropertyId(UUID.randomUUID());
        booking.setGuestId(UUID.randomUUID());
        booking.setCheckIn(LocalDate.now().plusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(100)); // Exceeds 90 night max
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setRatePlanId(UUID.randomUUID());
        booking.setRoomTypeIds(Set.of(UUID.randomUUID()));

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    // ==================== Special Characters Validation Tests ====================

    @Test
    void testNoSpecialCharactersValidator() {
        NoSpecialCharactersValidator validator = new NoSpecialCharactersValidator();
        NoSpecialCharacters annotation = createNoSpecialCharsAnnotation(NoSpecialCharacters.Mode.RELAXED);
        validator.initialize(annotation);

        assertThat(validator.isValid("Valid Text 123", null)).isTrue();
        assertThat(validator.isValid("Text with-dash", null)).isTrue();
        assertThat(validator.isValid("user@example.com", null)).isTrue();
    }

    @Test
    void testDangerousCharactersRejected() {
        NoSpecialCharactersValidator validator = new NoSpecialCharactersValidator();
        NoSpecialCharacters annotation = createNoSpecialCharsAnnotation(NoSpecialCharacters.Mode.RELAXED);
        validator.initialize(annotation);

        assertThat(validator.isValid("'; DROP TABLE--", null)).isFalse();
        assertThat(validator.isValid("<script>alert('xss')</script>", null)).isFalse();
        assertThat(validator.isValid("text; rm -rf /", null)).isFalse();
        assertThat(validator.isValid("value|command", null)).isFalse();
    }

    // ==================== Boundary Value Tests ====================

    @Test
    void testAdultsMinBoundary() {
        BookingCreateRequest booking = createValidBooking();
        booking.setAdults(0);

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testAdultsMaxBoundary() {
        BookingCreateRequest booking = createValidBooking();
        booking.setAdults(11);

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testChildrenNegativeRejected() {
        BookingCreateRequest booking = createValidBooking();
        booking.setChildren(-1);

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void testOversizedRoomTypeListRejected() {
        BookingCreateRequest booking = createValidBooking();
        Set<UUID> tooManyRoomTypes = Stream.generate(UUID::randomUUID)
                .limit(11)
                .collect(java.util.stream.Collectors.toSet());
        booking.setRoomTypeIds(tooManyRoomTypes);

        Set<ConstraintViolation<BookingCreateRequest>> violations = validator.validate(booking);
        assertThat(violations).isNotEmpty();
    }

    // ==================== Helper Methods ====================

    private BookingCreateRequest createValidBooking() {
        BookingCreateRequest booking = new BookingCreateRequest();
        booking.setPropertyId(UUID.randomUUID());
        booking.setGuestId(UUID.randomUUID());
        booking.setCheckIn(LocalDate.now().plusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(5));
        booking.setAdults(2);
        booking.setChildren(0);
        booking.setRatePlanId(UUID.randomUUID());
        booking.setRoomTypeIds(Set.of(UUID.randomUUID()));
        return booking;
    }

    private ValidPhoneNumber createPhoneAnnotation(boolean required) {
        return new ValidPhoneNumber() {
            @Override
            public String message() {
                return "Invalid phone number";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public boolean required() {
                return required;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return ValidPhoneNumber.class;
            }
        };
    }

    private NoSpecialCharacters createNoSpecialCharsAnnotation(NoSpecialCharacters.Mode mode) {
        return new NoSpecialCharacters() {
            @Override
            public String message() {
                return "Invalid characters";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public String allowedChars() {
                return "";
            }

            @Override
            public Mode mode() {
                return mode;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return NoSpecialCharacters.class;
            }
        };
    }
}
