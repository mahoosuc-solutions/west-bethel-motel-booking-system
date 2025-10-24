package com.westbethel.motel_booking.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Database Health Indicator Tests")
class DatabaseHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Connection connection;

    private DatabaseHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DatabaseHealthIndicator(dataSource, jdbcTemplate);
    }

    @Test
    @DisplayName("Should return UP when database is healthy")
    void shouldReturnUpWhenDatabaseIsHealthy() throws SQLException {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("database");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails()).containsKey("queryTime");
        assertThat(health.getDetails()).containsKey("connectionValid");

        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
        verify(connection).close();
    }

    @Test
    @DisplayName("Should return DOWN when query fails")
    void shouldReturnDownWhenQueryFails() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new RuntimeException("Connection failed"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).asString().contains("Connection failed");
    }

    @Test
    @DisplayName("Should return DOWN when connection is invalid")
    void shouldReturnDownWhenConnectionIsInvalid() throws SQLException {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(dataSource.getConnection()).thenThrow(new SQLException("Cannot connect"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    @DisplayName("Should return DOWN when query returns unexpected result")
    void shouldReturnDownWhenQueryReturnsUnexpectedResult() throws SQLException {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).asString()
            .contains("Query returned unexpected result");
    }
}
