package com.westbethel.motel_booking.reporting.service.impl;

import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.reporting.model.ReportRequest;
import com.westbethel.motel_booking.reporting.model.ReportResult;
import com.westbethel.motel_booking.reporting.service.ReportingService;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.reservation.repository.BookingRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultReportingService implements ReportingService {

    private static final EnumSet<BookingStatus> REPORTABLE_STATUSES = EnumSet.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.CHECKED_OUT);

    private final BookingRepository bookingRepository;

    public DefaultReportingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public ReportResult generate(ReportRequest request) {
        return switch (request.getType()) {
            case DAILY_OCCUPANCY -> buildDailyOccupancyReport(request.getPropertyId(), request.getFromDate(), request.getToDate());
            case REVENUE_SUMMARY -> buildRevenueSummaryReport(request.getPropertyId(), request.getFromDate(), request.getToDate());
            default -> throw new UnsupportedOperationException("Report type not yet implemented");
        };
    }

    private ReportResult buildDailyOccupancyReport(UUID propertyId, LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepository.findOverlappingBookings(
                propertyId,
                REPORTABLE_STATUSES,
                from,
                to.plusDays(1));

        Map<LocalDate, Long> occupancyByDate = bookings.stream()
                .flatMap(booking -> bookingStayDates(booking).stream())
                .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

        StringBuilder csv = new StringBuilder("date,occupied_rooms\n");
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            long occupied = occupancyByDate.getOrDefault(cursor, 0L);
            csv.append(cursor).append(',').append(occupied).append('\n');
            cursor = cursor.plusDays(1);
        }
        return toCsvResult("daily-occupancy", csv.toString());
    }

    private ReportResult buildRevenueSummaryReport(UUID propertyId, LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepository.findOverlappingBookings(
                propertyId,
                REPORTABLE_STATUSES,
                from,
                to.plusDays(1));

        Map<LocalDate, Double> revenueByDate = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getCheckIn,
                        Collectors.summingDouble(booking -> booking.getTotalAmount().getAmount().doubleValue())));

        StringBuilder csv = new StringBuilder("date,revenue\n");
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            double revenue = revenueByDate.getOrDefault(cursor, 0.0);
            csv.append(cursor).append(',').append(String.format("%.2f", revenue)).append('\n');
            cursor = cursor.plusDays(1);
        }
        return toCsvResult("revenue-summary", csv.toString());
    }

    private List<LocalDate> bookingStayDates(Booking booking) {
        LocalDate cursor = booking.getCheckIn();
        LocalDate end = booking.getCheckOut();
        return cursor.datesUntil(end).collect(Collectors.toList());
    }

    private ReportResult toCsvResult(String prefix, String csv) {
        return ReportResult.builder()
                .reportId(prefix + '-' + UUID.randomUUID())
                .generatedAt(OffsetDateTime.now())
                .format("text/csv")
                .payload(csv.getBytes(StandardCharsets.UTF_8))
                .build();
    }
}
