package com.westbethel.motel_booking.guest.api.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuestResponseDto {

    private final UUID id;
    private final String customerNumber;
    private final ContactDetailsDto contactDetails;
    private final AddressDto address;
    private final String preferences;
    private final Boolean marketingOptIn;
    private final UUID loyaltyProfileId;

    @Getter
    @Builder
    public static class ContactDetailsDto {
        private final String email;
        private final String phone;
    }

    @Getter
    @Builder
    public static class AddressDto {
        private final String line1;
        private final String line2;
        private final String city;
        private final String state;
        private final String postalCode;
        private final String country;
    }
}
