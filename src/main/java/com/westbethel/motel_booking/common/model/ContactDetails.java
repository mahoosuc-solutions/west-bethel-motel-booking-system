package com.westbethel.motel_booking.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactDetails {

    @Column(name = "contact_email")
    private String email;

    @Column(name = "contact_phone")
    private String phone;
}
