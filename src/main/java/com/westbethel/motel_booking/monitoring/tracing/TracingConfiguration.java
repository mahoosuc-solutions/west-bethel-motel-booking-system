package com.westbethel.motel_booking.monitoring.tracing;

import brave.Tracing;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed Tracing Configuration
 *
 * Configures Brave/Zipkin for distributed tracing across the application.
 * Enables trace context propagation for debugging and performance monitoring.
 */
@Slf4j
@Configuration
public class TracingConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${management.tracing.sampling.probability:1.0}")
    private double samplingProbability;

    /**
     * Configure sampling strategy
     * Default: 100% sampling for development, should be reduced in production
     */
    @Bean
    public Sampler defaultSampler() {
        log.info("Configuring trace sampler with probability: {}", samplingProbability);

        if (samplingProbability >= 1.0) {
            return Sampler.ALWAYS_SAMPLE;
        } else if (samplingProbability <= 0.0) {
            return Sampler.NEVER_SAMPLE;
        } else {
            return Sampler.create((float) samplingProbability);
        }
    }

    /**
     * Configure B3 propagation for trace context
     * B3 is the standard for distributed tracing
     */
    @Bean
    public Propagation.Factory propagationFactory() {
        return B3Propagation.FACTORY;
    }

    /**
     * Configure custom span tags
     */
    @Bean
    public brave.handler.SpanHandler spanHandler() {
        return new brave.handler.SpanHandler() {
            @Override
            public boolean end(brave.handler.MutableSpan span, brave.Span.Kind kind, long timestamp) {
                // Add custom tags to all spans
                span.tag("service", applicationName);
                return true;
            }
        };
    }
}
