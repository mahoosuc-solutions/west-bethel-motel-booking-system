package com.westbethel.motel_booking.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator that prevents injection attacks by disallowing dangerous special characters.
 */
public class NoSpecialCharactersValidator implements ConstraintValidator<NoSpecialCharacters, String> {

    // Dangerous characters commonly used in injection attacks
    private static final String DANGEROUS_CHARS = "'\";<>&|`$\\{}[]";

    private NoSpecialCharacters.Mode mode;
    private String allowedChars;

    @Override
    public void initialize(NoSpecialCharacters constraintAnnotation) {
        this.mode = constraintAnnotation.mode();
        this.allowedChars = constraintAnnotation.allowedChars();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Use @NotBlank separately if empty should be invalid
        }

        // Check for dangerous characters
        for (char c : DANGEROUS_CHARS.toCharArray()) {
            if (value.indexOf(c) >= 0 && allowedChars.indexOf(c) < 0) {
                return false;
            }
        }

        // Apply mode-specific validation
        Pattern pattern = (mode == NoSpecialCharacters.Mode.STRICT)
            ? getStrictPattern()
            : getRelaxedPattern();

        // Add custom allowed chars to pattern if specified
        String regex = pattern.pattern();
        if (!allowedChars.isEmpty()) {
            // Escape special regex characters in allowedChars
            String escaped = Pattern.quote(allowedChars);
            regex = regex.replace("]", escaped + "]");
        }

        return Pattern.compile(regex).matcher(value).matches();
    }

    private Pattern getStrictPattern() {
        // Only letters (including unicode), numbers, and spaces
        return Pattern.compile("^[\\p{L}\\p{N}\\s]*$");
    }

    private Pattern getRelaxedPattern() {
        // Letters, numbers, spaces, and safe punctuation
        return Pattern.compile("^[\\p{L}\\p{N}\\s.,\\-_@()]*$");
    }
}
