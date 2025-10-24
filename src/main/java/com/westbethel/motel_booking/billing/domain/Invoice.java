package com.westbethel.motel_booking.billing.domain;

import com.westbethel.motel_booking.common.model.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoices")
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Invoice {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_line_items", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<InvoiceLineItem> lineItems;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency", length = 3))
    })
    private Money subTotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "tax_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "tax_currency", length = 3))
    })
    private Money taxTotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "grand_total_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "grand_total_currency", length = 3))
    })
    private Money grandTotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance_due_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "balance_due_currency", length = 3))
    })
    private Money balanceDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    public void applyPayment(Money amount) {
        if (balanceDue == null || balanceDue.getAmount() == null) {
            return;
        }
        var newBalance = balanceDue.getAmount().subtract(amount.getAmount());
        var adjusted = newBalance.max(java.math.BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
        balanceDue = Money.builder()
                .amount(adjusted)
                .currency(balanceDue.getCurrency())
                .build();
        if (adjusted.signum() == 0) {
            status = InvoiceStatus.PAID;
        } else {
            status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void applyRefund(Money amount) {
        if (balanceDue == null || balanceDue.getAmount() == null) {
            balanceDue = amount.toBuilder().build();
        } else {
            var newBalance = balanceDue.getAmount().add(amount.getAmount())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            balanceDue = balanceDue.toBuilder()
                    .amount(newBalance)
                    .build();
        }
        status = InvoiceStatus.ISSUED;
    }
}
