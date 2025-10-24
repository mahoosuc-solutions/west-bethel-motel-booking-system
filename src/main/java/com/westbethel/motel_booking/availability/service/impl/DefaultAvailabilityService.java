package com.westbethel.motel_booking.availability.service.impl;

import com.westbethel.motel_booking.availability.model.AvailabilityQuery;
import com.westbethel.motel_booking.availability.model.AvailabilityResult;
import com.westbethel.motel_booking.availability.model.AvailabilityResult.NightlyRate;
import com.westbethel.motel_booking.availability.model.AvailabilityResult.RoomTypeAvailability;
import com.westbethel.motel_booking.availability.service.AvailabilityService;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.RoomStatus;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.inventory.repository.RoomRepository;
import com.westbethel.motel_booking.inventory.repository.RoomTypeRepository;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.property.repository.PropertyRepository;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.reservation.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.RoundingMode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultAvailabilityService implements AvailabilityService {

    private static final EnumSet<BookingStatus> ACTIVE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.HOLD,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN);

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public DefaultAvailabilityService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            BookingRepository bookingRepository) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Cacheable(cacheNames = "availability")
    public AvailabilityResult searchAvailability(AvailabilityQuery query) {
        validateQuery(query);

        Property property = propertyRepository.findById(query.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        List<RoomType> roomTypes = resolveRoomTypes(property.getId(), query.getRoomTypeCodes());
        if (roomTypes.isEmpty()) {
            throw new IllegalArgumentException("No matching room types for property");
        }

        Set<UUID> bookedRoomIds = bookingRepository.findOverlappingBookings(
                        property.getId(),
                        ACTIVE_BOOKING_STATUSES,
                        query.getStartDate(),
                        query.getEndDate())
                .stream()
                .flatMap(booking -> booking.getRoomIds().stream())
                .collect(Collectors.toSet());

        List<RoomTypeAvailability> availability = new ArrayList<>();
        for (RoomType roomType : roomTypes) {
            long totalRooms = roomRepository.findByPropertyIdAndRoomTypeIdAndStatus(
                            property.getId(), roomType.getId(), RoomStatus.AVAILABLE)
                    .stream()
                    .filter(room -> !bookedRoomIds.contains(room.getId()))
                    .count();

            availability.add(RoomTypeAvailability.builder()
                    .roomTypeCode(roomType.getCode())
                    .availableRooms(Math.toIntExact(totalRooms))
                    .nightlyRates(buildNightlyRates(query, property, roomType.getBaseRate()))
                    .build());
        }

        return AvailabilityResult.builder()
                .roomTypes(availability)
                .build();
    }

    private List<RoomType> resolveRoomTypes(UUID propertyId, Set<String> roomTypeCodes) {
        if (roomTypeCodes == null || roomTypeCodes.isEmpty()) {
            return roomTypeRepository.findByPropertyId(propertyId);
        }
        return roomTypeRepository.findByPropertyIdAndCodeIn(propertyId, roomTypeCodes);
    }

    private List<NightlyRate> buildNightlyRates(AvailabilityQuery query, Property property, Money baseRate) {
        List<NightlyRate> nightlyRates = new ArrayList<>();
        LocalDate current = query.getStartDate();
        while (current.isBefore(query.getEndDate())) {
            nightlyRates.add(NightlyRate.builder()
                    .stayDate(current)
                    .currency(resolveCurrency(property, baseRate))
                    .amount(resolveAmount(baseRate))
                    .build());
            current = current.plusDays(1);
        }
        return nightlyRates;
    }

    private String resolveCurrency(Property property, Money baseRate) {
        if (baseRate != null && baseRate.getCurrency() != null) {
            return baseRate.getCurrency().getCurrencyCode();
        }
        return property.getDefaultCurrency().getCurrencyCode();
    }

    private String resolveAmount(Money baseRate) {
        BigDecimal amount = baseRate != null && baseRate.getAmount() != null
                ? baseRate.getAmount()
                : BigDecimal.ZERO;
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void validateQuery(AvailabilityQuery query) {
        Objects.requireNonNull(query.getPropertyId(), "propertyId is required");
        Objects.requireNonNull(query.getStartDate(), "startDate is required");
        Objects.requireNonNull(query.getEndDate(), "endDate is required");
        if (!query.getEndDate().isAfter(query.getStartDate())) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }
    }
}
