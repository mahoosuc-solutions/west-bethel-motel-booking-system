package com.westbethel.motel_booking.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that check-in date is before check-out date.
 * Applied at the class level to access both fields.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    String message() default "Check-in date must be before check-out date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String checkInField() default "checkIn";
    String checkOutField() default "checkOut";
    int minNights() default 1;
    int maxNights() default 365;
}
