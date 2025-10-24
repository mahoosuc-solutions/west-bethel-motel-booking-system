package com.westbethel.motel_booking.billing.model;

import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.PaymentMethod;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCommand {

    private final UUID invoiceId;
    private final String paymentToken;
    private final Money amount;
    private final String initiatedBy;
    private final PaymentMethod method;
}
