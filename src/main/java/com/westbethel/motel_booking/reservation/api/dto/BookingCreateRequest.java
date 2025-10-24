package com.westbethel.motel_booking.reservation.api.dto;

import com.westbethel.motel_booking.common.validation.NoSpecialCharacters;
import com.westbethel.motel_booking.common.validation.ValidDateRange;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ValidDateRange(checkInField = "checkIn", checkOutField = "checkOut", minNights = 1, maxNights = 90)
public class BookingCreateRequest {

    @NotNull(message = "Property ID is required")
    private UUID propertyId;

    @NotNull(message = "Guest ID is required")
    private UUID guestId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date must be today or in the future")
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOut;

    @NotNull(message = "Number of adults is required")
    @Min(value = 1, message = "At least 1 adult is required")
    @Max(value = 10, message = "Maximum 10 adults per booking")
    private Integer adults;

    @NotNull(message = "Number of children is required")
    @Min(value = 0, message = "Number of children cannot be negative")
    @Max(value = 10, message = "Maximum 10 children per booking")
    private Integer children;

    @NotNull(message = "Rate plan ID is required")
    private UUID ratePlanId;

    @NotEmpty(message = "At least one room type must be selected")
    @Size(max = 10, message = "Maximum 10 room types per booking")
    private Set<UUID> roomTypeIds;

    @Size(max = 20, message = "Maximum 20 add-ons per booking")
    private Set<UUID> addonIds;

    @Size(max = 500, message = "Payment token too long")
    @NoSpecialCharacters(mode = NoSpecialCharacters.Mode.STRICT)
    private String paymentToken;

    @Size(max = 100, message = "Source cannot exceed 100 characters")
    @NoSpecialCharacters
    private String source;
}
