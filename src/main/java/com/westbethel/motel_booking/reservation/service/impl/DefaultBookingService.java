package com.westbethel.motel_booking.reservation.service.impl;

import com.westbethel.motel_booking.common.model.BookingChannel;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import com.westbethel.motel_booking.common.model.RoomStatus;
import com.westbethel.motel_booking.inventory.domain.Room;
import com.westbethel.motel_booking.inventory.domain.RoomType;
import com.westbethel.motel_booking.inventory.repository.RoomRepository;
import com.westbethel.motel_booking.inventory.repository.RoomTypeRepository;
import com.westbethel.motel_booking.pricing.domain.RatePlan;
import com.westbethel.motel_booking.pricing.model.PricingContext;
import com.westbethel.motel_booking.pricing.model.PricingQuote;
import com.westbethel.motel_booking.pricing.repository.RatePlanRepository;
import com.westbethel.motel_booking.pricing.service.PricingService;
import com.westbethel.motel_booking.property.domain.Property;
import com.westbethel.motel_booking.property.repository.PropertyRepository;
import com.westbethel.motel_booking.reservation.domain.Booking;
import com.westbethel.motel_booking.reservation.model.BookingRequest;
import com.westbethel.motel_booking.reservation.model.BookingResponse;
import com.westbethel.motel_booking.reservation.model.CancellationRequest;
import com.westbethel.motel_booking.reservation.repository.BookingRepository;
import com.westbethel.motel_booking.reservation.service.BookingService;
import com.westbethel.motel_booking.guest.repository.GuestRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingService implements BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final GuestRepository guestRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RatePlanRepository ratePlanRepository;
    private final PricingService pricingService;

    public DefaultBookingService(
            BookingRepository bookingRepository,
            PropertyRepository propertyRepository,
            GuestRepository guestRepository,
            RoomTypeRepository roomTypeRepository,
            RoomRepository roomRepository,
            RatePlanRepository ratePlanRepository,
            PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.guestRepository = guestRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.pricingService = pricingService;
    }

    @Override
    public PricingQuote quote(BookingRequest request) {
        validateRequest(request);
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        return pricingService.quote(buildPricingContext(request, property));
    }

    @Override
    public BookingResponse create(BookingRequest request) {
        validateRequest(request);

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        guestRepository.findById(request.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException("Guest not found"));

        RatePlan ratePlan = ratePlanRepository.findByPropertyIdAndId(property.getId(), request.getRatePlanId())
                .orElseThrow(() -> new IllegalArgumentException("Rate plan not found for property"));

        Set<UUID> allocatedRoomIds = allocateRooms(property.getId(), request);

        PricingQuote quote = pricingService.quote(buildPricingContext(request, property));

        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .propertyId(property.getId())
                .reference(generateReference(property.getCode()))
                .guestId(request.getGuestId())
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.INITIATED)
                .channel(BookingChannel.DIRECT)
                .source(request.getSource())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .adults(request.getAdults())
                .children(request.getChildren())
                .ratePlanId(ratePlan.getId())
                .roomIds(allocatedRoomIds)
                .totalAmount(quote.getTotalAmount())
                .balanceDue(quote.getTotalAmount())
                .createdAt(OffsetDateTime.now())
                .build();

        booking.markConfirmed();

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Override
    public BookingResponse amend(String confirmationNumber, BookingRequest request) {
        Booking booking = bookingRepository.findByReference(confirmationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + confirmationNumber));

        allocateRooms(booking.getPropertyId(), request); // ensure availability before amendments
        booking.markConfirmed();
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Override
    public BookingResponse cancel(CancellationRequest request) {
        Booking booking = bookingRepository.findByReference(request.getConfirmationNumber())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + request.getConfirmationNumber()));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }

        booking.markCancelled();
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    private PricingContext buildPricingContext(BookingRequest request, Property property) {
        return PricingContext.builder()
                .propertyId(property.getId())
                .ratePlanId(request.getRatePlanId())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .adults(request.getAdults())
                .children(request.getChildren())
                .guestId(request.getGuestId())
                .roomTypeIds(request.getRoomTypeIds())
                .build();
    }

    private Set<UUID> allocateRooms(UUID propertyId, BookingRequest request) {
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                propertyId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.HOLD, BookingStatus.CHECKED_IN),
                request.getCheckIn(),
                request.getCheckOut());

        Set<UUID> bookedRoomIds = overlapping.stream()
                .flatMap(booking -> booking.getRoomIds().stream())
                .collect(Collectors.toSet());

        Set<UUID> allocated = new HashSet<>();
        for (UUID roomTypeId : request.getRoomTypeIds()) {
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("Room type not found"));

            if (!roomType.getPropertyId().equals(propertyId)) {
                throw new IllegalArgumentException("Room type does not belong to the property");
            }

            List<Room> candidates = roomRepository.findByPropertyIdAndRoomTypeIdAndStatus(
                    propertyId, roomType.getId(), RoomStatus.AVAILABLE);

            Room room = candidates.stream()
                    .filter(candidate -> !bookedRoomIds.contains(candidate.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available rooms for room type"));

            allocated.add(room.getId());
            bookedRoomIds.add(room.getId());
        }
        return allocated;
    }

    private void validateRequest(BookingRequest request) {
        if (request.getCheckIn() == null || request.getCheckOut() == null) {
            throw new IllegalArgumentException("Stay dates are required");
        }
        if (!request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        if (request.getRoomTypeIds() == null || request.getRoomTypeIds().isEmpty()) {
            throw new IllegalArgumentException("At least one room type is required");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .confirmationNumber(booking.getReference())
                .status(booking.getStatus())
                .build();
    }

    private String generateReference(String propertyCode) {
        return propertyCode.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
