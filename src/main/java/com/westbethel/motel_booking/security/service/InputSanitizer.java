package com.westbethel.motel_booking.security.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for sanitizing user input to prevent injection attacks.
 * Uses allowlist approach where possible.
 */
@Service
public class InputSanitizer {

    // Patterns for detecting various injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('.+--)|(--)|(;)|(\\bOR\\b.+[=>])|" +
            "(\\bUNION\\b.+\\bSELECT\\b)|(\\bDROP\\b.+\\bTABLE\\b)|" +
            "(\\bINSERT\\b.+\\bINTO\\b)|(\\bDELETE\\b.+\\bFROM\\b)|" +
            "(\\bUPDATE\\b.+\\bSET\\b)|(\\bEXEC\\b)|(\\bEXECUTE\\b)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(<script[^>]*>)|(</script>)|" +
            "(<iframe[^>]*>)|(</iframe>)|" +
            "(javascript:)|(onerror\\s*=)|(onload\\s*=)|" +
            "(eval\\()|(alert\\()",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./)|(\\.\\\\)|(%2e%2e/)|(\\\\\\.\\.)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
            "(;\\s*(rm|del|format|shutdown))|(\\||&&|`|\\$\\()",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize HTML input by removing script tags and dangerous attributes.
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove script tags
        sanitized = sanitized.replaceAll("<script[^>]*>.*?</script>", "");
        sanitized = sanitized.replaceAll("<iframe[^>]*>.*?</iframe>", "");

        // Remove event handlers
        sanitized = sanitized.replaceAll("on\\w+\\s*=\\s*['\"][^'\"]*['\"]", "");
        sanitized = sanitized.replaceAll("on\\w+\\s*=\\s*[^\\s>]*", "");

        // Remove javascript: protocol
        sanitized = sanitized.replaceAll("javascript:", "");

        // Remove dangerous tags
        sanitized = sanitized.replaceAll("<(object|embed|applet|meta|link|style)[^>]*>.*?</\\1>", "");

        return sanitized;
    }

    /**
     * Sanitize SQL input by escaping special characters.
     */
    public String sanitizeSql(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Escape single quotes
        String sanitized = input.replace("'", "''");

        // Escape backslashes
        sanitized = sanitized.replace("\\", "\\\\");

        // Remove SQL comments
        sanitized = sanitized.replaceAll("--.*", "");
        sanitized = sanitized.replaceAll("/\\*.*?\\*/", "");

        return sanitized;
    }

    /**
     * Validate and sanitize file paths to prevent directory traversal.
     */
    public String sanitizeFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // Remove directory traversal sequences
        String sanitized = path.replaceAll("\\.\\./", "");
        sanitized = sanitized.replaceAll("\\.\\\\", "");
        sanitized = sanitized.replaceAll("%2e%2e/", "");
        sanitized = sanitized.replaceAll("%2e%2e\\\\", "");

        // Remove leading slashes/backslashes
        sanitized = sanitized.replaceAll("^[/\\\\]+", "");

        // Allow only alphanumeric, dots, hyphens, underscores, and forward slashes
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9./_-]", "");

        return sanitized;
    }

    /**
     * Validate and sanitize URLs.
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // Only allow http and https protocols
        if (!url.matches("^https?://.*")) {
            return "";
        }

        // Remove javascript: and data: protocols
        String sanitized = url.replaceAll("javascript:", "");
        sanitized = sanitized.replaceAll("data:", "");

        return sanitized;
    }

    /**
     * Remove or escape special characters from free text.
     */
    public String sanitizeFreeText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove control characters
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\n\r\t]]", "");

        // Limit length to prevent overflow
        if (sanitized.length() > 10000) {
            sanitized = sanitized.substring(0, 10000);
        }

        return sanitized;
    }

    /**
     * Check if input contains SQL injection attempts.
     */
    public boolean containsSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains XSS attempts.
     */
    public boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains path traversal attempts.
     */
    public boolean containsPathTraversal(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Check if input contains command injection attempts.
     */
    public boolean containsCommandInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Validate input against allowlist pattern.
     */
    public boolean isAllowlisted(String input, Pattern allowedPattern) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        return allowedPattern.matcher(input).matches();
    }

    /**
     * Sanitize alphanumeric input (letters, numbers, spaces only).
     */
    public String sanitizeAlphanumeric(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("[^a-zA-Z0-9\\s]", "");
    }

    /**
     * Sanitize email address.
     */
    public String sanitizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        // Only allow valid email characters
        String sanitized = email.replaceAll("[^a-zA-Z0-9@.+_-]", "");

        // Limit length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized.toLowerCase();
    }

    /**
     * Comprehensive input validation check.
     */
    public ValidationResult validate(String input) {
        ValidationResult result = new ValidationResult();

        if (input == null || input.isEmpty()) {
            result.setValid(true);
            return result;
        }

        if (containsSqlInjection(input)) {
            result.setValid(false);
            result.addViolation("Potential SQL injection detected");
        }

        if (containsXss(input)) {
            result.setValid(false);
            result.addViolation("Potential XSS attack detected");
        }

        if (containsPathTraversal(input)) {
            result.setValid(false);
            result.addViolation("Potential path traversal attack detected");
        }

        if (containsCommandInjection(input)) {
            result.setValid(false);
            result.addViolation("Potential command injection detected");
        }

        return result;
    }

    /**
     * Result of input validation.
     */
    public static class ValidationResult {
        private boolean valid = true;
        private final StringBuilder violations = new StringBuilder();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public void addViolation(String violation) {
            if (violations.length() > 0) {
                violations.append("; ");
            }
            violations.append(violation);
        }

        public String getViolations() {
            return violations.toString();
        }
    }
}
