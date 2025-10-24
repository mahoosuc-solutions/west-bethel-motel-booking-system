package com.westbethel.motel_booking.pricing.domain;

import com.westbethel.motel_booking.common.model.BookingChannel;
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
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rate_plans")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatePlan {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private BookingChannel channel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rate_plan_room_types", joinColumns = @JoinColumn(name = "rate_plan_id"))
    @Column(name = "room_type_id")
    private Set<UUID> eligibleRoomTypeIds;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "default_rate_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "default_rate_currency", length = 3))
    })
    private Money defaultRate;

    @Column(name = "pricing_rules", length = 2048)
    private String pricingRules;

    @Column(name = "cancellation_policy", length = 1024)
    private String cancellationPolicy;

    @Column(name = "stay_restrictions", length = 512)
    private String stayRestrictions;
}
