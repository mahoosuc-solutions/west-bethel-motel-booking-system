package com.westbethel.motel_booking.monitoring.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Metrics Configuration for Prometheus Integration
 *
 * Configures custom metric naming, tags, and filters for the monitoring system.
 * Ensures consistent metrics collection across the application.
 */
@Configuration
public class MetricsConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${SPRING_PROFILES_ACTIVE:prod}")
    private String environment;

    /**
     * Customize MeterRegistry with common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(Arrays.asList(
                Tag.of("application", applicationName),
                Tag.of("environment", environment),
                Tag.of("region", System.getenv().getOrDefault("AWS_REGION", "us-east-1"))
            ))
            .meterFilter(MeterFilter.denyNameStartsWith("jvm.threads.states"))
            .meterFilter(MeterFilter.denyNameStartsWith("process.files"))
            .meterFilter(MeterFilter.maximumAllowableTags("http.server.requests", "uri", 100, MeterFilter.deny()));
    }

    /**
     * Configure metric naming conventions
     */
    @Bean
    public MeterFilter renameMeters() {
        return MeterFilter.renameTag("http.server.requests", "uri", "endpoint");
    }

    /**
     * Filter out unnecessary metrics to reduce cardinality
     */
    @Bean
    public MeterFilter filterMetrics() {
        return MeterFilter.deny(id -> {
            String name = id.getName();
            // Deny metrics that might have high cardinality
            return name.startsWith("tomcat.sessions") ||
                   name.startsWith("logback.events");
        });
    }
}
