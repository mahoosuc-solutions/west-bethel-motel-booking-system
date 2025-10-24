package com.westbethel.motel_booking.billing.api;

import com.westbethel.motel_booking.billing.api.dto.PaymentAuthorizeRequest;
import com.westbethel.motel_booking.billing.api.dto.PaymentRefundRequest;
import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;
import com.westbethel.motel_booking.billing.service.PaymentService;
import com.westbethel.motel_booking.common.model.Money;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/invoices/{invoiceId}/payments/authorize")
    public ResponseEntity<PaymentResult> authorize(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody PaymentAuthorizeRequest request) {
        PaymentCommand command = PaymentCommand.builder()
                .invoiceId(invoiceId)
                .paymentToken(request.getPaymentToken())
                .amount(toMoney(request.getAmount().getAmount(), request.getAmount().getCurrency()))
                .initiatedBy(request.getInitiatedBy())
                .method(request.getMethod())
                .build();
        return ResponseEntity.ok(paymentService.authorize(command));
    }

    @PostMapping("/payments/{paymentId}/capture")
    public ResponseEntity<PaymentResult> capture(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.capture(paymentId));
    }

    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<PaymentResult> refund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PaymentRefundRequest request) {
        PaymentCommand command = PaymentCommand.builder()
                .invoiceId(null)
                .amount(toMoney(request.getAmount().getAmount(), request.getAmount().getCurrency()))
                .method(request.getMethod())
                .initiatedBy("SYSTEM")
                .paymentToken("REFUND")
                .build();
        return ResponseEntity.ok(paymentService.refund(paymentId, command));
    }

    @PostMapping("/payments/{paymentId}/void")
    public ResponseEntity<PaymentResult> voidAuthorization(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.voidAuthorization(paymentId));
    }

    private Money toMoney(BigDecimal amount, String currency) {
        return Money.builder()
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .currency(Currency.getInstance(currency))
                .build();
    }
}
