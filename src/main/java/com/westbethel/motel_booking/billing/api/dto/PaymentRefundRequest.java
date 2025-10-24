package com.westbethel.motel_booking.billing.api.dto;

import com.westbethel.motel_booking.common.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRefundRequest {

    @NotNull
    private PaymentMethod method;

    @Valid
    @NotNull
    private PaymentAmountDto amount;
}
