package com.westbethel.motel_booking.guest.domain;

import com.westbethel.motel_booking.common.model.Address;
import com.westbethel.motel_booking.common.model.ContactDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guests")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Guest {

    @Id
    private UUID id;

    @Column(name = "customer_number", unique = true, length = 32)
    private String customerNumber;

    @Embedded
    private ContactDetails contactDetails;

    @Embedded
    private Address address;

    @Column(name = "preferences", length = 1024)
    private String preferences;

    @Column(name = "marketing_opt_in", nullable = false)
    private Boolean marketingOptIn;

    @Column(name = "loyalty_profile_id")
    private UUID loyaltyProfileId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
