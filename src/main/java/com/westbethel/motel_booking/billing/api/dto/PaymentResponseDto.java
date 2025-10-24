package com.westbethel.motel_booking.billing.api.dto;

import com.westbethel.motel_booking.common.model.PaymentStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResponseDto {

    private final UUID paymentId;
    private final PaymentStatus status;
    private final String processorReference;
    private final String failureReason;
}
