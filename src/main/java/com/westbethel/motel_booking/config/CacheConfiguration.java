package com.westbethel.motel_booking.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Multi-Level Caching Configuration
 *
 * Implements a two-tier caching strategy:
 * - L1 Cache (Caffeine): Fast in-memory cache for frequently accessed data
 * - L2 Cache (Redis): Distributed cache shared across application instances
 *
 * Performance Targets:
 * - Cache hit ratio: >70%
 * - L1 cache hit time: <1ms
 * - L2 cache hit time: <10ms
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    /**
     * Cache names and their TTL configurations
     */
    public static final String AVAILABILITY_CACHE = "availability";
    public static final String ROOM_TYPES_CACHE = "room-types";
    public static final String RATE_PLANS_CACHE = "rate-plans";
    public static final String PROMOTIONS_CACHE = "promotions";
    public static final String USER_PROFILES_CACHE = "user-profiles";
    public static final String PROPERTY_CONFIG_CACHE = "property-config";
    public static final String BOOKINGS_CACHE = "bookings";
    public static final String PRICING_CACHE = "pricing";

    /**
     * L1 Cache (Caffeine) - Primary in-memory cache
     *
     * Caffeine is a high-performance, near-optimal caching library.
     * It provides automatic cache eviction, size-based limits, and TTL support.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            // Availability cache: 5 minutes TTL, up to 10,000 entries
            buildCache(AVAILABILITY_CACHE, 5, TimeUnit.MINUTES, 10_000),

            // Room types: 24 hours TTL, up to 1,000 entries (static data)
            buildCache(ROOM_TYPES_CACHE, 24, TimeUnit.HOURS, 1_000),

            // Rate plans: 1 hour TTL, up to 5,000 entries
            buildCache(RATE_PLANS_CACHE, 1, TimeUnit.HOURS, 5_000),

            // Promotions: 1 hour TTL, up to 2,000 entries
            buildCache(PROMOTIONS_CACHE, 1, TimeUnit.HOURS, 2_000),

            // User profiles: 30 minutes TTL, up to 5,000 entries
            buildCache(USER_PROFILES_CACHE, 30, TimeUnit.MINUTES, 5_000),

            // Property config: 1 hour TTL, up to 500 entries (static data)
            buildCache(PROPERTY_CONFIG_CACHE, 1, TimeUnit.HOURS, 500),

            // Bookings: 10 minutes TTL, up to 20,000 entries
            buildCache(BOOKINGS_CACHE, 10, TimeUnit.MINUTES, 20_000),

            // Pricing calculations: 15 minutes TTL, up to 15,000 entries
            buildCache(PRICING_CACHE, 15, TimeUnit.MINUTES, 15_000)
        ));

        return cacheManager;
    }

    /**
     * L2 Cache (Redis) - Distributed cache for multi-instance deployments
     *
     * Redis provides a distributed cache that's shared across all application instances.
     * This ensures cache consistency in horizontal scaling scenarios.
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration(AVAILABILITY_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(ROOM_TYPES_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(24)))
            .withCacheConfiguration(RATE_PLANS_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(PROMOTIONS_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(USER_PROFILES_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(PROPERTY_CONFIG_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(BOOKINGS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(PRICING_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .transactionAware()
            .build();
    }

    /**
     * Build a Caffeine cache with specified TTL and maximum size
     *
     * @param name Cache name
     * @param duration TTL duration value
     * @param timeUnit TTL time unit
     * @param maxSize Maximum number of entries
     * @return Configured CaffeineCache
     */
    private CaffeineCache buildCache(String name, long duration, TimeUnit timeUnit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(duration, timeUnit)
            .maximumSize(maxSize)
            .recordStats() // Enable statistics for monitoring
            .build());
    }
}
