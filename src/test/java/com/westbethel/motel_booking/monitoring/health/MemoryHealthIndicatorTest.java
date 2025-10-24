package com.westbethel.motel_booking.monitoring.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Memory Health Indicator Tests")
class MemoryHealthIndicatorTest {

    private MemoryHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new MemoryHealthIndicator();
    }

    @Test
    @DisplayName("Should return health status with memory details")
    void shouldReturnHealthStatusWithMemoryDetails() {
        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails()).containsKey("usedMemory");
        assertThat(health.getDetails()).containsKey("freeMemory");
        assertThat(health.getDetails()).containsKey("totalMemory");
        assertThat(health.getDetails()).containsKey("maxMemory");
        assertThat(health.getDetails()).containsKey("usagePercentage");
    }

    @Test
    @DisplayName("Should format bytes correctly")
    void shouldFormatBytesCorrectly() {
        // When
        Health health = healthIndicator.health();

        // Then
        String usedMemory = (String) health.getDetails().get("usedMemory");
        assertThat(usedMemory).matches("\\d+\\.\\d+ [KMGTPE]?B");
    }

    @Test
    @DisplayName("Should include status message")
    void shouldIncludeStatusMessage() {
        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails().get("status")).isNotNull();
        assertThat(health.getDetails().get("status").toString())
            .containsAnyOf("OK", "WARNING", "CRITICAL");
    }
}
