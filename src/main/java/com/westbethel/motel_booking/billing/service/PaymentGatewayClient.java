package com.westbethel.motel_booking.billing.service;

import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;

public interface PaymentGatewayClient {

    PaymentResult authorize(PaymentCommand command);

    PaymentResult capture(String processorReference);

    PaymentResult refund(String processorReference, PaymentCommand command);

    PaymentResult voidAuthorization(String processorReference);
}
