package com.westbethel.motel_booking.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a currency code is one of the supported currencies.
 * Prevents injection attacks through currency field.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
@Documented
public @interface ValidCurrency {
    String message() default "Currency code must be one of: USD, EUR, GBP, CAD";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
