package com.westbethel.motel_booking.common.validation;

import com.westbethel.motel_booking.common.model.SupportedCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for currency codes using allowlist approach.
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull separately if null should be invalid
        }

        // Sanitize: remove any whitespace and limit length
        String sanitized = value.trim();
        if (sanitized.length() != 3) {
            return false;
        }

        // Validate against allowlist
        return SupportedCurrency.isSupported(sanitized);
    }
}
