package com.westbethel.motel_booking.billing.api.mapper;

import com.westbethel.motel_booking.billing.api.dto.PaymentAmountDto;
import com.westbethel.motel_booking.billing.api.dto.PaymentAuthorizeRequest;
import com.westbethel.motel_booking.billing.api.dto.PaymentResponseDto;
import com.westbethel.motel_booking.billing.model.PaymentCommand;
import com.westbethel.motel_booking.billing.model.PaymentResult;
import com.westbethel.motel_booking.common.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentCommand toCommand(UUID invoiceId, PaymentAuthorizeRequest request) {
        return PaymentCommand.builder()
                .invoiceId(invoiceId)
                .paymentToken(request.getPaymentToken())
                .amount(toMoney(request.getAmount()))
                .initiatedBy(request.getInitiatedBy())
                .method(request.getMethod())
                .build();
    }

    public Money toMoney(PaymentAmountDto dto) {
        return Money.builder()
                .amount(dto.getAmount().setScale(2, RoundingMode.HALF_UP))
                .currency(Currency.getInstance(dto.getCurrency()))
                .build();
    }

    public PaymentAmountDto toAmountDto(Money money) {
        PaymentAmountDto dto = new PaymentAmountDto();
        dto.setAmount(money.getAmount());
        dto.setCurrency(money.getCurrency().getCurrencyCode());
        return dto;
    }

    public PaymentResponseDto toResponseDto(PaymentResult result) {
        return PaymentResponseDto.builder()
                .paymentId(result.getPaymentId())
                .status(result.getStatus())
                .processorReference(result.getProcessorReference())
                .failureReason(result.getFailureReason())
                .build();
    }
}
