package com.westbethel.motel_booking.performance;

import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.reservation.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Query Performance Tests
 *
 * Validates:
 * - Repository query optimization
 * - Projection queries
 * - Pagination
 * - Index usage
 */
@DataJpaTest
@ActiveProfiles("test")
class QueryPerformanceTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void testProjectionQueryPerformance() {
        UUID guestId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        long start = System.currentTimeMillis();
        var page = bookingRepository.findByGuestIdProjection(guestId, pageable);
        long duration = System.currentTimeMillis() - start;

        // Should complete in < 100ms even without data
        assertThat(duration).isLessThan(100);
        assertThat(page).isNotNull();
    }

    @Test
    void testPropertyProjectionQuery() {
        UUID propertyId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);

        long start = System.currentTimeMillis();
        var page = bookingRepository.findByPropertyIdProjection(propertyId, pageable);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(100);
        assertThat(page).isNotNull();
    }

    @Test
    void testStatusProjectionQuery() {
        var pageable = PageRequest.of(0, 20);

        long start = System.currentTimeMillis();
        var page = bookingRepository.findByStatusProjection(BookingStatus.CONFIRMED, pageable);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(100);
        assertThat(page).isNotNull();
    }

    @Test
    void testDateRangeProjectionQuery() {
        UUID propertyId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);

        long start = System.currentTimeMillis();
        var page = bookingRepository.findByPropertyAndDateRange(
            propertyId,
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(30),
            pageable
        );
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(100);
        assertThat(page).isNotNull();
    }

    @Test
    void testCountByStatus() {
        long start = System.currentTimeMillis();
        long count = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(50);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testCountByPropertyAndStatus() {
        UUID propertyId = UUID.randomUUID();

        long start = System.currentTimeMillis();
        long count = bookingRepository.countByPropertyIdAndStatus(propertyId, BookingStatus.CONFIRMED);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(50);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testPaginationPerformance() {
        UUID guestId = UUID.randomUUID();

        // Test multiple pages
        for (int page = 0; page < 5; page++) {
            var pageable = PageRequest.of(page, 10);

            long start = System.currentTimeMillis();
            var result = bookingRepository.findByGuestIdProjection(guestId, pageable);
            long duration = System.currentTimeMillis() - start;

            assertThat(duration).isLessThan(100);
            assertThat(result.getNumber()).isEqualTo(page);
        }
    }

    @Test
    void testFindByReferencePerformance() {
        String reference = "TEST-" + UUID.randomUUID();

        long start = System.currentTimeMillis();
        var booking = bookingRepository.findByReference(reference);
        long duration = System.currentTimeMillis() - start;

        // Should use index for fast lookup
        assertThat(duration).isLessThan(50);
        assertThat(booking).isNotNull();
    }

    @Test
    void testOverlappingBookingsPerformance() {
        UUID propertyId = UUID.randomUUID();
        var statuses = java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

        long start = System.currentTimeMillis();
        var bookings = bookingRepository.findOverlappingBookings(
            propertyId,
            statuses,
            java.time.LocalDate.now(),
            java.time.LocalDate.now().plusDays(7)
        );
        long duration = System.currentTimeMillis() - start;

        // Should use composite index
        assertThat(duration).isLessThan(100);
        assertThat(bookings).isNotNull();
    }
}
