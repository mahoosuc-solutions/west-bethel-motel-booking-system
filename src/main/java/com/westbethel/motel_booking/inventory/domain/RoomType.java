package com.westbethel.motel_booking.inventory.domain;

import com.westbethel.motel_booking.common.model.Money;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_types")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomType {

    @Id
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "bed_configuration", length = 128)
    private String bedConfiguration;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_type_amenities", joinColumns = @JoinColumn(name = "room_type_id"))
    @Column(name = "amenity", length = 64)
    private Set<String> amenities;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "base_rate_amount", precision = 15, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "base_rate_currency", length = 3))
    })
    private Money baseRate;
}
