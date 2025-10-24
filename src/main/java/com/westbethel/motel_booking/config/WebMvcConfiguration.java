package com.westbethel.motel_booking.config;

import com.westbethel.motel_booking.performance.PerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 *
 * Registers:
 * - Performance interceptor for request monitoring
 * - CORS configuration (if needed)
 * - Custom argument resolvers
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/actuator/**", "/error");
    }
}
