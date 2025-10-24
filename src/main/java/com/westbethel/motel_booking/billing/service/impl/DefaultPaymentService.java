package com.westbethel.motel_booking.billing.service.impl;

import com.westbethel.motel_booking.billing.domain.Invoice;
import com.westbethel.motel_booking.billing.domain.InvoiceStatus;
import com.westbethel.motel_booking.billing.domain.Payment;
import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;
import com.westbethel.motel_booking.billing.repository.InvoiceRepository;
import com.westbethel.motel_booking.billing.repository.PaymentRepository;
import com.westbethel.motel_booking.billing.service.PaymentGatewayClient;
import com.westbethel.motel_booking.billing.service.PaymentService;
import com.westbethel.motel_booking.common.model.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPaymentService implements PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;

    public DefaultPaymentService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PaymentGatewayClient paymentGatewayClient) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Override
    public PaymentResult authorize(PaymentCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        validateInvoiceOpen(invoice);

        PaymentResult gatewayResult = paymentGatewayClient.authorize(command);
        Payment payment = Payment.builder()
                .id(gatewayResult.getPaymentId())
                .invoiceId(invoice.getId())
                .method(command.getMethod())
                .processor("SIMULATED")
                .amount(command.getAmount())
                .status(gatewayResult.getStatus())
                .authCode(gatewayResult.getProcessorReference())
                .processedAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);
        return PaymentResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .processorReference(payment.getAuthCode())
                .build();
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Only authorized payments can be captured");
        }

        PaymentResult gatewayResult = paymentGatewayClient.capture(payment.getAuthCode());
        payment = payment.toBuilder()
                .status(PaymentStatus.CAPTURED)
                .processedAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);

        invoice.applyPayment(payment.getAmount());
        invoiceRepository.save(invoice);
        return PaymentResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .processorReference(gatewayResult.getProcessorReference())
                .build();
    }

    @Override
    public PaymentResult refund(UUID paymentId, PaymentCommand command) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Only captured payments can be refunded");
        }

        PaymentResult gatewayResult = paymentGatewayClient.refund(payment.getAuthCode(), command);
        payment = payment.toBuilder()
                .status(PaymentStatus.REFUNDED)
                .processedAt(OffsetDateTime.now())
                .failureReason(null)
                .build();
        paymentRepository.save(payment);

        invoice.applyRefund(payment.getAmount());
        invoiceRepository.save(invoice);
        return PaymentResult.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .processorReference(gatewayResult.getProcessorReference())
                .build();
    }

    @Override
    public PaymentResult voidAuthorization(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Only authorized payments can be voided");
        }

        PaymentResult gatewayResult = paymentGatewayClient.voidAuthorization(payment.getAuthCode());
        paymentRepository.save(payment.toBuilder()
                .status(PaymentStatus.VOIDED)
                .processedAt(OffsetDateTime.now())
                .build());
        return PaymentResult.builder()
                .paymentId(payment.getId())
                .status(PaymentStatus.VOIDED)
                .processorReference(gatewayResult.getProcessorReference())
                .build();
    }

    private void validateInvoiceOpen(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is not open for payment");
        }
    }
}
