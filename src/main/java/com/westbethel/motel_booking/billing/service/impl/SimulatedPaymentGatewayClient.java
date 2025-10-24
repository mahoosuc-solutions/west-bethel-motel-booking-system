package com.westbethel.motel_booking.billing.service.impl;

import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;
import com.westbethel.motel_booking.billing.service.PaymentGatewayClient;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SimulatedPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentResult authorize(PaymentCommand command) {
        return PaymentResult.builder()
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.AUTHORIZED)
                .processorReference(generateReference("AUTH"))
                .build();
    }

    @Override
    public PaymentResult capture(String processorReference) {
        return PaymentResult.builder()
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.CAPTURED)
                .processorReference(processorReference)
                .build();
    }

    @Override
    public PaymentResult refund(String processorReference, PaymentCommand command) {
        return PaymentResult.builder()
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.REFUNDED)
                .processorReference(processorReference)
                .build();
    }

    @Override
    public PaymentResult voidAuthorization(String processorReference) {
        return PaymentResult.builder()
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.VOIDED)
                .processorReference(processorReference)
                .build();
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
