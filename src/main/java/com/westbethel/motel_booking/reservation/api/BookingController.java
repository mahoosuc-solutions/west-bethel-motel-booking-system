package com.westbethel.motel_booking.reservation.api;

import com.westbethel.motel_booking.reservation.api.dto.BookingCancelRequest;
import com.westbethel.motel_booking.reservation.api.dto.BookingCreateRequest;
import com.westbethel.motel_booking.reservation.api.dto.BookingResponseDto;
import com.westbethel.motel_booking.reservation.api.mapper.ReservationMapper;
import com.westbethel.motel_booking.reservation.model.BookingResponse;
import com.westbethel.motel_booking.reservation.model.CancellationRequest;
import com.westbethel.motel_booking.reservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@PreAuthorize("hasRole('USER')")
public class BookingController {

    private final BookingService bookingService;
    private final ReservationMapper mapper;

    public BookingController(BookingService bookingService, ReservationMapper mapper) {
        this.bookingService = bookingService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> create(@Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.create(mapper.toBookingRequest(request));
        return ResponseEntity.ok(mapper.toDto(response));
    }

    @PostMapping("/{confirmationNumber}/cancel")
    public ResponseEntity<BookingResponseDto> cancel(
            @PathVariable String confirmationNumber,
            @Valid @RequestBody BookingCancelRequest request) {

        CancellationRequest cancellation = CancellationRequest.builder()
                .bookingId(null)
                .confirmationNumber(confirmationNumber)
                .reason(request.getReason())
                .requestedBy(request.getRequestedBy())
                .build();

        BookingResponse response = bookingService.cancel(cancellation);
        return ResponseEntity.ok(mapper.toDto(response));
    }
}
