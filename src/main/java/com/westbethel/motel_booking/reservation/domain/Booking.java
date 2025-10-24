package com.westbethel.motel_booking.reservation.domain;

import com.westbethel.motel_booking.common.model.BookingChannel;
import com.westbethel.motel_booking.common.model.BookingStatus;
import com.westbethel.motel_booking.common.model.Money;
import com.westbethel.motel_booking.common.model.PaymentStatus;
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
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bookings")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(nullable = false, unique = true, length = 32)
    private String reference;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private BookingChannel channel;

    @Column(name = "source", length = 64)
    private String source;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "adults", nullable = false)
    private Integer adults;

    @Column(name = "children", nullable = false)
    private Integer children;

    @Column(name = "rate_plan_id", nullable = false)
    private UUID ratePlanId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_rooms", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "room_id")
    private Set<UUID> roomIds;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", length = 3))
    })
    private Money totalAmount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance_due_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "balance_due_currency", length = 3))
    })
    private Money balanceDue;

    @Column(name = "notes", length = 2048)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    public void markCancelled() {
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markConfirmed() {
        this.status = BookingStatus.CONFIRMED;
        this.updatedAt = OffsetDateTime.now();
    }
}
