package com.westbethel.motel_booking.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validator for date ranges in booking requests.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String checkInField;
    private String checkOutField;
    private int minNights;
    private int maxNights;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.checkInField = constraintAnnotation.checkInField();
        this.checkOutField = constraintAnnotation.checkOutField();
        this.minNights = constraintAnnotation.minNights();
        this.maxNights = constraintAnnotation.maxNights();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            LocalDate checkIn = getFieldValue(value, checkInField);
            LocalDate checkOut = getFieldValue(value, checkOutField);

            if (checkIn == null || checkOut == null) {
                return true; // Let @NotNull handle null checks
            }

            // Check that check-out is after check-in
            if (!checkOut.isAfter(checkIn)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Check-out date must be after check-in date")
                        .addPropertyNode(checkOutField)
                        .addConstraintViolation();
                return false;
            }

            // Check minimum nights
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < minNights) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Minimum stay is " + minNights + " night(s)")
                        .addPropertyNode(checkOutField)
                        .addConstraintViolation();
                return false;
            }

            // Check maximum nights
            if (nights > maxNights) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "Maximum stay is " + maxNights + " nights")
                        .addPropertyNode(checkOutField)
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDate getFieldValue(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (LocalDate) field.get(object);
    }
}
