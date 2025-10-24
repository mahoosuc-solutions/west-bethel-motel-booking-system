package com.westbethel.motel_booking.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator for international phone numbers.
 * Accepts various international formats but sanitizes input to prevent injection.
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Pattern for international phone numbers
    // Allows: +, digits, spaces, hyphens, parentheses
    // Must start with + followed by 1-3 digits (country code), then 7-15 additional digits
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+[1-9]\\d{0,2}[\\s.-]?(?:\\(\\d{1,4}\\)[\\s.-]?)?[\\d\\s.-]{7,15}$"
    );

    private boolean required;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return !required; // If not required, null/blank is valid
        }

        // Sanitize: trim and check length to prevent overflow attacks
        String sanitized = value.trim();
        if (sanitized.length() > 20) {
            return false; // Too long for a valid phone number
        }

        // Check for dangerous characters (injection prevention)
        if (containsDangerousCharacters(sanitized)) {
            return false;
        }

        // Validate format
        return PHONE_PATTERN.matcher(sanitized).matches();
    }

    private boolean containsDangerousCharacters(String value) {
        // Check for SQL injection, XSS, command injection characters
        String dangerous = "'\";<>&|`$\\{}[]";
        for (char c : dangerous.toCharArray()) {
            if (value.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
