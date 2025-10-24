package com.westbethel.motel_booking.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * E2E Test Configuration using TestContainers for PostgreSQL and Redis.
 * Provides isolated test environments with real database and cache instances.
 */
@TestConfiguration
@Testcontainers
public class E2ETestConfiguration {

    private static final PostgreSQLContainer<?> postgresContainer;
    private static final GenericContainer<?> redisContainer;

    static {
        // Initialize PostgreSQL container
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("motel_booking_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgresContainer.start();

        // Initialize Redis container
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redisContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        // Redis properties
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");

        // JWT secret for testing
        registry.add("jwt.secret", () -> "testSecretKeyForE2ETestingPurposesOnlyMinimum256BitsRequired");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("jwt.refresh-expiration", () -> "86400000");

        // Email configuration for testing (mock)
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "3025");
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        registry.add("notification.from-address", () -> "test@westbethelmotel.com");

        // Disable external integrations
        registry.add("twilio.enabled", () -> "false");
        registry.add("monitoring.alerts.enabled", () -> "false");

        // Frontend URL
        registry.add("frontend.url", () -> "http://localhost:3000");
    }

    @Bean
    @Primary
    public PostgreSQLContainer<?> postgresContainer() {
        return postgresContainer;
    }

    @Bean
    @Primary
    public GenericContainer<?> redisContainer() {
        return redisContainer;
    }

    /**
     * Clean up containers on shutdown
     */
    public static void cleanup() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
        if (redisContainer != null && redisContainer.isRunning()) {
            redisContainer.stop();
        }
    }
}
