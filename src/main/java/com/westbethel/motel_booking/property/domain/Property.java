package com.westbethel.motel_booking.property.domain;

import com.westbethel.motel_booking.common.model.Address;
import com.westbethel.motel_booking.common.model.ContactDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "properties")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Property {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "timezone", nullable = false, length = 64)
    private ZoneId timezone;

    @Column(name = "default_currency", nullable = false, length = 3)
    private Currency defaultCurrency;

    @Embedded
    private Address address;

    @Embedded
    private ContactDetails contactDetails;
}
