package com.westbethel.motel_booking.monitoring.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Custom Database Health Indicator
 *
 * Checks database connectivity and connection pool status.
 * Provides detailed health information for monitoring.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // Test database connectivity
            long startTime = System.currentTimeMillis();
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long queryTime = System.currentTimeMillis() - startTime;

            if (result == null || result != 1) {
                return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", "Query returned unexpected result")
                    .build();
            }

            // Get connection pool information
            try (Connection connection = dataSource.getConnection()) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("validationQuery", "SELECT 1")
                    .withDetail("queryTime", queryTime + "ms")
                    .withDetail("connectionValid", connection.isValid(1))
                    .build();
            }

        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        } catch (Exception e) {
            log.error("Unexpected error during database health check", e);
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", "Unexpected error: " + e.getMessage())
                .withException(e)
                .build();
        }
    }
}
