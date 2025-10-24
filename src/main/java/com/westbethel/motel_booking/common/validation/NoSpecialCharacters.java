package com.westbethel.motel_booking.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a string does not contain special characters that could be used for injection attacks.
 * Allows letters, numbers, spaces, and basic punctuation only.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoSpecialCharactersValidator.class)
@Documented
public @interface NoSpecialCharacters {
    String message() default "Field contains invalid characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Allow additional characters beyond the default safe set.
     */
    String allowedChars() default "";

    /**
     * Validation mode: STRICT (alphanumeric + spaces only) or RELAXED (allows basic punctuation)
     */
    Mode mode() default Mode.RELAXED;

    enum Mode {
        STRICT,   // Only letters, numbers, spaces
        RELAXED   // Also allows: . , - _ @ ( )
    }
}
