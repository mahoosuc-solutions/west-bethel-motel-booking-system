package com.westbethel.motel_booking.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator for UUID strings.
 */
public class UUIDValidator implements ConstraintValidator<ValidUUID, String> {

    // Standard UUID format: 8-4-4-4-12 hexadecimal characters
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull separately if null should be invalid
        }

        // Sanitize and validate length
        String sanitized = value.trim();
        if (sanitized.length() != 36) { // UUID is always 36 characters with hyphens
            return false;
        }

        // Validate format
        return UUID_PATTERN.matcher(sanitized).matches();
    }
}
