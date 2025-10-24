package com.westbethel.motel_booking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Configuration Validator Component
 *
 * Validates all required environment variables on application startup.
 * Implements fail-fast principle: if critical configuration is missing or invalid,
 * the application will not start.
 *
 * Security validations include:
 * - Presence of all required environment variables
 * - JWT secret strength (minimum 256 bits)
 * - Database connection validation
 * - Redis connection validation
 * - Password strength recommendations
 */
@Component
public class ConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

    private final Environment environment;

    // Required configuration properties
    @Value("${spring.datasource.username:#{null}}")
    private String databaseUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String databasePassword;

    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;

    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validates configuration immediately after construction
     * This ensures the application fails fast if configuration is invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        logger.info("=".repeat(80));
        logger.info("Starting Configuration Validation");
        logger.info("Active Profile: {}", activeProfile);
        logger.info("=".repeat(80));

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Skip strict validation for test profile
        if ("test".equals(activeProfile)) {
            logger.info("Test profile detected - skipping strict validation");
            logger.info("=".repeat(80));
            return;
        }

        // Validate database configuration
        validateDatabaseConfiguration(errors, warnings);

        // Validate Redis configuration (skip for dev if cache is disabled)
        if (!"dev".equals(activeProfile) || "redis".equals(environment.getProperty("spring.cache.type"))) {
            validateRedisConfiguration(errors, warnings);
        }

        // Validate JWT configuration
        validateJwtConfiguration(errors, warnings);

        // Validate email configuration (warnings only)
        validateEmailConfiguration(warnings);

        // Log warnings
        if (!warnings.isEmpty()) {
            logger.warn("-".repeat(80));
            logger.warn("Configuration Warnings:");
            warnings.forEach(warning -> logger.warn("  ⚠  {}", warning));
            logger.warn("-".repeat(80));
        }

        // Fail fast if there are errors
        if (!errors.isEmpty()) {
            logger.error("=".repeat(80));
            logger.error("CONFIGURATION VALIDATION FAILED");
            logger.error("=".repeat(80));
            errors.forEach(error -> logger.error("  ✗  {}", error));
            logger.error("=".repeat(80));
            logger.error("Please fix the configuration errors above and restart the application.");
            logger.error("See docs/ENVIRONMENT_VARIABLES.md for detailed configuration guide.");
            logger.error("=".repeat(80));

            throw new IllegalStateException(
                "Application configuration is invalid. " +
                "Please check the logs above for details. " +
                "See docs/ENVIRONMENT_VARIABLES.md for configuration guide."
            );
        }

        logger.info("=".repeat(80));
        logger.info("✓ Configuration Validation Successful");
        logger.info("=".repeat(80));
    }

    /**
     * Additional validation when application is fully ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application is ready and all configurations are valid");
        logConfigurationSummary();
    }

    /**
     * Validates database configuration
     */
    private void validateDatabaseConfiguration(List<String> errors, List<String> warnings) {
        logger.info("Validating database configuration...");

        if (databaseUsername == null || databaseUsername.trim().isEmpty()) {
            errors.add("DATABASE_USERNAME is required but not set");
        } else {
            logger.info("  ✓ Database username configured");
        }

        if (databasePassword == null || databasePassword.trim().isEmpty()) {
            errors.add("DATABASE_PASSWORD is required but not set");
        } else {
            logger.info("  ✓ Database password configured");

            // Check password strength
            if (databasePassword.length() < 12) {
                warnings.add("Database password is short (less than 12 characters). Consider using a stronger password.");
            }

            // Warn about default/weak passwords
            if (databasePassword.equals("password") ||
                databasePassword.equals("admin") ||
                databasePassword.equals("change-me") ||
                databasePassword.equals("devpassword123")) {
                warnings.add("Database password appears to be a default/weak password. Use a strong password in production.");
            }
        }

        String databaseUrl = environment.getProperty("spring.datasource.url");
        if (databaseUrl != null) {
            logger.info("  ✓ Database URL: {}", maskDatabaseUrl(databaseUrl));
        }
    }

    /**
     * Validates Redis configuration
     */
    private void validateRedisConfiguration(List<String> errors, List<String> warnings) {
        logger.info("Validating Redis configuration...");

        if (redisPassword == null || redisPassword.trim().isEmpty()) {
            if ("prod".equals(activeProfile)) {
                errors.add("REDIS_PASSWORD is required for production but not set");
            } else {
                warnings.add("REDIS_PASSWORD is not set. This is required for production.");
            }
        } else {
            logger.info("  ✓ Redis password configured");

            // Check password strength
            if (redisPassword.length() < 16) {
                warnings.add("Redis password is short (less than 16 characters). Consider using a stronger password.");
            }

            // Warn about default/weak passwords
            if (redisPassword.equals("devredispass")) {
                warnings.add("Redis password appears to be a default password. Use a strong password in production.");
            }
        }

        // Check SSL configuration for production
        if ("prod".equals(activeProfile)) {
            Boolean sslEnabled = environment.getProperty("spring.data.redis.ssl.enabled", Boolean.class);
            if (sslEnabled == null || !sslEnabled) {
                warnings.add("Redis SSL is not enabled. Strongly recommended for production environments.");
            } else {
                logger.info("  ✓ Redis SSL enabled");
            }
        }
    }

    /**
     * Validates JWT configuration
     */
    private void validateJwtConfiguration(List<String> errors, List<String> warnings) {
        logger.info("Validating JWT configuration...");

        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            errors.add("JWT_SECRET is required but not set");
            return;
        }

        logger.info("  ✓ JWT secret configured");

        // Validate JWT secret strength (minimum 256 bits = 32 bytes)
        try {
            byte[] decodedSecret = Base64.getDecoder().decode(jwtSecret);
            int bits = decodedSecret.length * 8;

            if (bits < 256) {
                errors.add(String.format(
                    "JWT_SECRET is too weak (%d bits). Minimum 256 bits (32 bytes) required for security. " +
                    "Use ./scripts/generate-secrets.sh to generate a secure secret.",
                    bits
                ));
            } else {
                logger.info("  ✓ JWT secret strength: {} bits", bits);
            }
        } catch (IllegalArgumentException e) {
            errors.add("JWT_SECRET is not a valid Base64 encoded string. " +
                      "Use ./scripts/generate-secrets.sh to generate a valid secret.");
        }

        // Warn about development secrets in production
        if ("prod".equals(activeProfile)) {
            String devSecret = "ZGV2ZWxvcG1lbnRzZWNyZXRrZXlmb3J0ZXN0aW5nb25seWRvbm90dXNlaW5wcm9kdWN0aW9uMTIzNDU2Nzg5MA==";
            if (devSecret.equals(jwtSecret)) {
                errors.add("JWT_SECRET is using the development default secret. " +
                          "NEVER use development secrets in production! " +
                          "Generate a new secret with ./scripts/generate-secrets.sh");
            }
        }
    }

    /**
     * Validates email configuration (warnings only)
     */
    private void validateEmailConfiguration(List<String> warnings) {
        logger.info("Validating email configuration...");

        String mailUsername = environment.getProperty("spring.mail.username");
        String mailPassword = environment.getProperty("spring.mail.password");

        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            warnings.add("MAIL_USERNAME is not set. Email features may not work.");
        } else {
            logger.info("  ✓ Email username configured");
        }

        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            warnings.add("MAIL_PASSWORD is not set. Email features may not work.");
        } else {
            logger.info("  ✓ Email password configured");
        }
    }

    /**
     * Logs a summary of the configuration
     */
    private void logConfigurationSummary() {
        logger.info("-".repeat(80));
        logger.info("Configuration Summary:");
        logger.info("  Profile: {}", activeProfile);
        logger.info("  Database: {}", maskDatabaseUrl(environment.getProperty("spring.datasource.url")));
        logger.info("  Redis: {}:{}",
            environment.getProperty("spring.data.redis.host", "localhost"),
            environment.getProperty("spring.data.redis.port", "6379"));
        logger.info("  Server Port: {}", environment.getProperty("server.port", "8080"));
        logger.info("-".repeat(80));
    }

    /**
     * Masks sensitive information in database URL
     */
    private String maskDatabaseUrl(String url) {
        if (url == null) {
            return "not configured";
        }
        // Mask password if present in URL
        return url.replaceAll(":[^:@]+@", ":***@");
    }
}
