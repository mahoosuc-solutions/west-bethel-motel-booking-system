# Cache Usage Guide
## West Bethel Motel Booking System

This guide provides practical examples for using caching in the application.

---

## Cache Annotations

### @Cacheable - Read from Cache

Use `@Cacheable` to cache method results:

```java
import org.springframework.cache.annotation.Cacheable;
import static com.westbethel.motel_booking.config.CacheConfiguration.*;

@Service
public class RoomTypeService {

    @Cacheable(value = ROOM_TYPES_CACHE, key = "#propertyId")
    public List<RoomType> findByProperty(UUID propertyId) {
        // This method result will be cached
        // Subsequent calls will return cached value
        return roomTypeRepository.findByPropertyId(propertyId);
    }

    @Cacheable(value = ROOM_TYPES_CACHE, key = "#id")
    public RoomType findById(UUID id) {
        return roomTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Room type not found"));
    }
}
```

### @CacheEvict - Remove from Cache

Use `@CacheEvict` to remove cached data when it changes:

```java
@Service
public class RoomTypeService {

    @CacheEvict(value = ROOM_TYPES_CACHE, key = "#id")
    public RoomType update(UUID id, RoomTypeRequest request) {
        // After update, remove old cached value
        RoomType roomType = findById(id);
        // ... update logic
        return roomTypeRepository.save(roomType);
    }

    @CacheEvict(value = ROOM_TYPES_CACHE, allEntries = true)
    public void deleteAll() {
        // Clear entire cache
        roomTypeRepository.deleteAll();
    }
}
```

### @CachePut - Update Cache

Use `@CachePut` to update cached value:

```java
@Service
public class BookingService {

    @CachePut(value = BOOKINGS_CACHE, key = "#result.id")
    public Booking updateStatus(UUID id, BookingStatus status) {
        // Update and cache the new value
        Booking booking = findById(id);
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }
}
```

### @Caching - Multiple Cache Operations

Use `@Caching` for multiple cache operations:

```java
@Service
public class RatePlanService {

    @Caching(
        cacheable = {
            @Cacheable(value = RATE_PLANS_CACHE, key = "#id")
        },
        evict = {
            @CacheEvict(value = PRICING_CACHE, allEntries = true)
        }
    )
    public RatePlan findById(UUID id) {
        // Cache in rate-plans, evict pricing cache
        return ratePlanRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rate plan not found"));
    }
}
```

---

## Cache Key Strategies

### Simple Key
```java
@Cacheable(value = AVAILABILITY_CACHE, key = "#propertyId")
public AvailabilityResult check(UUID propertyId, LocalDate date) {
    // Cache key: propertyId value
}
```

### Composite Key
```java
@Cacheable(value = AVAILABILITY_CACHE,
          key = "#propertyId + '-' + #checkIn + '-' + #checkOut")
public AvailabilityResult search(UUID propertyId, LocalDate checkIn, LocalDate checkOut) {
    // Cache key: "uuid-2024-01-01-2024-01-05"
}
```

### SpEL Expression
```java
@Cacheable(value = BOOKINGS_CACHE,
          key = "#booking.id",
          condition = "#booking.status == T(com.westbethel.motel_booking.common.model.BookingStatus).CONFIRMED")
public Booking save(Booking booking) {
    // Only cache if status is CONFIRMED
}
```

### Custom Key Generator
```java
@Cacheable(value = PRICING_CACHE, keyGenerator = "customKeyGenerator")
public PricingQuote calculate(PricingRequest request) {
    // Uses custom key generation logic
}
```

---

## Cache TTL Reference

| Cache Name | TTL | Max Entries | Typical Use Case |
|------------|-----|-------------|------------------|
| availability | 5 min | 10,000 | Room searches |
| room-types | 24 hours | 1,000 | Room catalog |
| rate-plans | 1 hour | 5,000 | Pricing |
| promotions | 1 hour | 2,000 | Active deals |
| user-profiles | 30 min | 5,000 | User data |
| property-config | 1 hour | 500 | Settings |
| bookings | 10 min | 20,000 | Reservations |
| pricing | 15 min | 15,000 | Price quotes |

---

## Manual Cache Operations

### Using CacheService

```java
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final CacheService cacheService;

    public void updateProperty(UUID id, PropertyRequest request) {
        // Update property
        Property property = propertyRepository.save(updated);

        // Manually evict related caches
        cacheService.evictCacheEntry(PROPERTY_CONFIG_CACHE, id);
        cacheService.evictCache(AVAILABILITY_CACHE); // Clear all availability
    }

    public void refreshPropertyCache(UUID id) {
        Property property = propertyRepository.findById(id)
            .orElseThrow();

        // Manually add to cache
        cacheService.putCacheEntry(PROPERTY_CONFIG_CACHE, id, property);
    }
}
```

### Cache Statistics

```java
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/statistics")
    public Map<String, CacheStatistics> getStatistics() {
        return cacheService.getCacheStatistics();
    }

    @GetMapping("/hit-ratio")
    public double getOverallHitRatio() {
        return cacheService.getOverallHitRatio();
    }

    @PostMapping("/evict/{cacheName}")
    public boolean evictCache(@PathVariable String cacheName) {
        return cacheService.evictCache(cacheName);
    }
}
```

---

## Best Practices

### 1. Cache What's Expensive
✅ **Do cache:**
- Database queries
- External API calls
- Complex calculations
- Static/semi-static data

❌ **Don't cache:**
- Simple object creation
- Already fast operations
- Highly volatile data
- User-specific sensitive data

### 2. Choose Appropriate TTL
- **Static data** (room types): 24 hours
- **Semi-static** (rate plans): 1 hour
- **Dynamic** (availability): 5 minutes
- **Volatile** (pricing): 15 minutes

### 3. Use Conditional Caching
```java
@Cacheable(value = BOOKINGS_CACHE,
          key = "#id",
          condition = "#status == 'CONFIRMED'",
          unless = "#result == null")
public Booking findById(UUID id, BookingStatus status) {
    // Only cache confirmed bookings
    // Don't cache null results
}
```

### 4. Evict on Updates
```java
@CacheEvict(value = RATE_PLANS_CACHE, key = "#id")
public RatePlan update(UUID id, RatePlanRequest request) {
    // Always evict when data changes
}
```

### 5. Monitor Cache Performance
```java
// Check hit ratio regularly
double hitRatio = cacheService.getOverallHitRatio();
if (hitRatio < 0.7) {
    log.warn("Cache hit ratio below target: {}", hitRatio);
}
```

---

## Common Patterns

### Pattern 1: Cache Aside
```java
public Property getProperty(UUID id) {
    // Try cache first
    Property cached = cacheService.getCacheEntry(PROPERTY_CONFIG_CACHE, id, Property.class);
    if (cached != null) {
        return cached;
    }

    // Cache miss - load from database
    Property property = propertyRepository.findById(id)
        .orElseThrow();

    // Update cache
    cacheService.putCacheEntry(PROPERTY_CONFIG_CACHE, id, property);

    return property;
}
```

### Pattern 2: Write Through
```java
@CachePut(value = BOOKINGS_CACHE, key = "#result.id")
public Booking createBooking(BookingRequest request) {
    // Save to database
    Booking booking = bookingRepository.save(newBooking);

    // Cache is automatically updated via @CachePut
    return booking;
}
```

### Pattern 3: Bulk Eviction
```java
@CacheEvict(value = {AVAILABILITY_CACHE, PRICING_CACHE}, allEntries = true)
public void updateRatePlan(UUID id, RatePlanRequest request) {
    // Clear multiple related caches
    ratePlanRepository.save(updated);
}
```

---

## Troubleshooting

### Cache Not Working
1. Check if `@EnableCaching` is present in configuration
2. Verify cache manager is configured
3. Ensure methods are called from outside the class (not `this.method()`)
4. Check SpEL expressions in annotations

### Cache Hit Ratio Low
1. Review TTL settings - may be too short
2. Check key generation - keys may not match
3. Verify data is cacheable (not too volatile)
4. Monitor cache size limits

### Memory Issues
1. Reduce cache size limits in configuration
2. Shorten TTL for large objects
3. Use projections instead of full entities
4. Consider Redis for distributed caching

### Stale Data
1. Ensure proper cache eviction on updates
2. Reduce TTL for frequently changing data
3. Add conditional caching (`condition` parameter)
4. Use versioning in cache keys

---

## Performance Tips

1. **Use Projections for Lists**
```java
@Cacheable(value = BOOKINGS_CACHE, key = "'list-' + #guestId")
public List<BookingListDto> findByGuestId(UUID guestId) {
    // Cache lightweight DTOs, not full entities
    return bookingRepository.findByGuestIdProjection(guestId)
        .stream()
        .map(BookingListDto::fromProjection)
        .toList();
}
```

2. **Cache Expensive Joins**
```java
@Cacheable(value = BOOKINGS_CACHE, key = "#id + '-details'")
public BookingDetails getBookingWithDetails(UUID id) {
    // Cache complex queries with multiple joins
    return bookingRepository.findByIdWithRoomsAndPayments(id);
}
```

3. **Paginate Cached Results**
```java
// Cache pages separately
@Cacheable(value = BOOKINGS_CACHE,
          key = "'page-' + #page + '-' + #size + '-' + #guestId")
public Page<BookingProjection> findByGuest(UUID guestId, int page, int size) {
    return bookingRepository.findByGuestIdProjection(
        guestId,
        PageRequest.of(page, size)
    );
}
```

---

## Monitoring Commands

### Check Cache Statistics
```bash
curl http://localhost:8080/actuator/caches
```

### View Metrics
```bash
curl http://localhost:8080/actuator/metrics/cache.hits
curl http://localhost:8080/actuator/metrics/cache.misses
```

### Prometheus Queries
```promql
# Cache hit rate
rate(cache_hits_total[5m]) / (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m]))

# Cache size
cache_size{cache="availability"}
```

---

## Additional Resources

- [Spring Cache Abstraction Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Caffeine Cache Guide](https://github.com/ben-manes/caffeine/wiki)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)

---

**Last Updated:** October 23, 2025
**Version:** 1.0
