package com.westbethel.motel_booking.monitoring.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom Redis Health Indicator
 *
 * Checks Redis connectivity and provides detailed status information.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            RedisConnection connection = redisConnectionFactory.getConnection();
            try {
                // Test Redis with PING command
                String pong = connection.ping();
                long responseTime = System.currentTimeMillis() - startTime;

                // Get Redis info
                String version = getRedisVersion(connection);

                return Health.up()
                    .withDetail("redis", "Connected")
                    .withDetail("ping", pong)
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("version", version)
                    .build();

            } finally {
                connection.close();
            }

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                .withDetail("redis", "Connection failed")
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }

    private String getRedisVersion(RedisConnection connection) {
        try {
            var info = connection.info("server");
            if (info != null && info.getProperty("redis_version") != null) {
                return info.getProperty("redis_version");
            }
        } catch (Exception e) {
            log.debug("Could not retrieve Redis version", e);
        }
        return "unknown";
    }
}
