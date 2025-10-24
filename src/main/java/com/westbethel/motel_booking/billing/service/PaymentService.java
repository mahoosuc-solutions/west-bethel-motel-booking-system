package com.westbethel.motel_booking.billing.service;

import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;
import java.util.UUID;

public interface PaymentService {

    PaymentResult authorize(PaymentCommand command);

    PaymentResult capture(UUID paymentId);

    PaymentResult refund(UUID paymentId, PaymentCommand command);

    PaymentResult voidAuthorization(UUID paymentId);
}
