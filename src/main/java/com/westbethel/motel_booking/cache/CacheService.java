package com.westbethel.motel_booking.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cache Management Service
 *
 * Provides utilities for:
 * - Cache eviction (single and all caches)
 * - Cache statistics and monitoring
 * - Cache warming on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;

    /**
     * Evict a specific cache by name
     *
     * @param cacheName Name of the cache to evict
     * @return true if cache was found and evicted, false otherwise
     */
    public boolean evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cache evicted: {}", cacheName);
            return true;
        }
        log.warn("Cache not found: {}", cacheName);
        return false;
    }

    /**
     * Evict all caches
     *
     * @return Number of caches evicted
     */
    public int evictAllCaches() {
        int count = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                count++;
            }
        }
        log.info("All caches evicted. Total: {}", count);
        return count;
    }

    /**
     * Get cache statistics for all caches
     *
     * @return Map of cache name to statistics
     */
    public Map<String, CacheStatistics> getCacheStatistics() {
        Map<String, CacheStatistics> statistics = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                statistics.put(cacheName, CacheStatistics.builder()
                    .cacheName(cacheName)
                    .hitCount(stats.hitCount())
                    .missCount(stats.missCount())
                    .hitRate(stats.hitRate())
                    .evictionCount(stats.evictionCount())
                    .loadSuccessCount(stats.loadSuccessCount())
                    .loadFailureCount(stats.loadFailureCount())
                    .totalLoadTime(stats.totalLoadTime())
                    .averageLoadPenalty(stats.averageLoadPenalty())
                    .estimatedSize(caffeineCache.getNativeCache().estimatedSize())
                    .build());
            }
        }

        return statistics;
    }

    /**
     * Get hit ratio for a specific cache
     *
     * @param cacheName Name of the cache
     * @return Hit ratio (0.0 to 1.0), or -1.0 if cache not found
     */
    public double getCacheHitRatio(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            return caffeineCache.getNativeCache().stats().hitRate();
        }
        return -1.0;
    }

    /**
     * Get overall hit ratio across all caches
     *
     * @return Overall hit ratio (0.0 to 1.0)
     */
    public double getOverallHitRatio() {
        long totalHits = 0;
        long totalRequests = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                CacheStats stats = caffeineCache.getNativeCache().stats();
                totalHits += stats.hitCount();
                totalRequests += stats.requestCount();
            }
        }

        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    /**
     * Evict a specific entry from a cache
     *
     * @param cacheName Name of the cache
     * @param key Key to evict
     * @return true if entry was found and evicted, false otherwise
     */
    public boolean evictCacheEntry(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Cache entry evicted from {}: {}", cacheName, key);
            return true;
        }
        return false;
    }

    /**
     * Put a value into a specific cache
     *
     * @param cacheName Name of the cache
     * @param key Cache key
     * @param value Value to cache
     */
    public void putCacheEntry(String cacheName, Object key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            log.debug("Cache entry added to {}: {}", cacheName, key);
        }
    }

    /**
     * Get a value from a specific cache
     *
     * @param cacheName Name of the cache
     * @param key Cache key
     * @param type Expected value type
     * @return Cached value or null if not found
     */
    public <T> T getCacheEntry(String cacheName, Object key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return cache.get(key, type);
        }
        return null;
    }

    /**
     * Cache statistics data class
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStatistics {
        private String cacheName;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long evictionCount;
        private long loadSuccessCount;
        private long loadFailureCount;
        private long totalLoadTime;
        private double averageLoadPenalty;
        private long estimatedSize;

        public long getTotalRequests() {
            return hitCount + missCount;
        }

        public String getFormattedHitRate() {
            return String.format("%.2f%%", hitRate * 100);
        }
    }
}
