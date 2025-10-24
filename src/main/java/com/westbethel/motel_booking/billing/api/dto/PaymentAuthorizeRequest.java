package com.westbethel.motel_booking.billing.api.dto;

import com.westbethel.motel_booking.common.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentAuthorizeRequest {

    @NotNull
    private PaymentMethod method;

    @NotBlank
    private String paymentToken;

    @Valid
    @NotNull
    private PaymentAmountDto amount;

    @NotBlank
    private String initiatedBy;
}
