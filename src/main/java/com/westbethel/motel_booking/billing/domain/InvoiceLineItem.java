package com.westbethel.motel_booking.billing.domain;

import com.westbethel.motel_booking.common.model.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceLineItem {

    @Column(name = "line_description", nullable = false)
    private String description;

    @Column(name = "line_quantity", nullable = false)
    private Integer quantity;

    @Embedded
    private Money amount;
}
