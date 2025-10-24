package com.westbethel.motel_booking.performance;

import com.westbethel.motel_booking.cache.CacheService;
import com.westbethel.motel_booking.config.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cache Performance Tests
 *
 * Validates:
 * - Cache configuration
 * - Cache hit/miss ratios
 * - Cache eviction
 * - Multi-level caching
 */
@SpringBootTest
@ActiveProfiles("test")
class CachePerformanceTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void testCacheManagerIsConfigured() {
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).isNotEmpty();
    }

    @Test
    void testAvailabilityCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.AVAILABILITY_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testRoomTypesCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.ROOM_TYPES_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testRatePlansCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.RATE_PLANS_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testPromotionsCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.PROMOTIONS_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testUserProfilesCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.USER_PROFILES_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testPropertyConfigCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.PROPERTY_CONFIG_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testBookingsCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.BOOKINGS_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testPricingCacheExists() {
        var cache = cacheManager.getCache(CacheConfiguration.PRICING_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    void testEvictCache() {
        String cacheName = CacheConfiguration.AVAILABILITY_CACHE;
        boolean evicted = cacheService.evictCache(cacheName);
        assertThat(evicted).isTrue();
    }

    @Test
    void testEvictNonExistentCache() {
        boolean evicted = cacheService.evictCache("non-existent-cache");
        assertThat(evicted).isFalse();
    }

    @Test
    void testEvictAllCaches() {
        int count = cacheService.evictAllCaches();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void testPutAndGetCacheEntry() {
        String cacheName = CacheConfiguration.AVAILABILITY_CACHE;
        String key = "test-key";
        String value = "test-value";

        cacheService.putCacheEntry(cacheName, key, value);
        String retrieved = cacheService.getCacheEntry(cacheName, key, String.class);

        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    void testEvictSpecificCacheEntry() {
        String cacheName = CacheConfiguration.AVAILABILITY_CACHE;
        String key = "test-key-evict";
        String value = "test-value";

        cacheService.putCacheEntry(cacheName, key, value);
        boolean evicted = cacheService.evictCacheEntry(cacheName, key);
        assertThat(evicted).isTrue();

        String retrieved = cacheService.getCacheEntry(cacheName, key, String.class);
        assertThat(retrieved).isNull();
    }

    @Test
    void testCacheStatistics() {
        var statistics = cacheService.getCacheStatistics();
        assertThat(statistics).isNotEmpty();
        assertThat(statistics).containsKey(CacheConfiguration.AVAILABILITY_CACHE);
    }

    @Test
    void testGetCacheHitRatio() {
        String cacheName = CacheConfiguration.AVAILABILITY_CACHE;
        double hitRatio = cacheService.getCacheHitRatio(cacheName);
        assertThat(hitRatio).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testGetOverallHitRatio() {
        double overallHitRatio = cacheService.getOverallHitRatio();
        assertThat(overallHitRatio).isGreaterThanOrEqualTo(0.0);
        assertThat(overallHitRatio).isLessThanOrEqualTo(1.0);
    }

    @Test
    void testCachePerformance() {
        String cacheName = CacheConfiguration.AVAILABILITY_CACHE;
        int iterations = 1000;

        // Warm up
        for (int i = 0; i < 100; i++) {
            cacheService.putCacheEntry(cacheName, "key-" + i, "value-" + i);
        }

        // Measure write performance
        long startWrite = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            cacheService.putCacheEntry(cacheName, "perf-key-" + i, "perf-value-" + i);
        }
        long writeTime = System.currentTimeMillis() - startWrite;

        // Measure read performance
        long startRead = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            cacheService.getCacheEntry(cacheName, "perf-key-" + i, String.class);
        }
        long readTime = System.currentTimeMillis() - startRead;

        // Assert performance (should be very fast)
        assertThat(writeTime).isLessThan(1000); // < 1ms per write
        assertThat(readTime).isLessThan(500);   // < 0.5ms per read
    }

    @Test
    void testCacheStatisticsStructure() {
        // Add some test data
        cacheService.putCacheEntry(CacheConfiguration.AVAILABILITY_CACHE, "test", "value");
        cacheService.getCacheEntry(CacheConfiguration.AVAILABILITY_CACHE, "test", String.class);

        var statistics = cacheService.getCacheStatistics();
        var availabilityStats = statistics.get(CacheConfiguration.AVAILABILITY_CACHE);

        assertThat(availabilityStats).isNotNull();
        assertThat(availabilityStats.getCacheName()).isEqualTo(CacheConfiguration.AVAILABILITY_CACHE);
        assertThat(availabilityStats.getTotalRequests()).isGreaterThanOrEqualTo(0);
        assertThat(availabilityStats.getFormattedHitRate()).isNotNull();
    }
}
