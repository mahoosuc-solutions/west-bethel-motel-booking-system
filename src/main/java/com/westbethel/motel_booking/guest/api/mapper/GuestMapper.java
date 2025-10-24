package com.westbethel.motel_booking.guest.api.mapper;

import com.westbethel.motel_booking.common.model.Address;
import com.westbethel.motel_booking.common.model.ContactDetails;
import com.westbethel.motel_booking.guest.api.dto.GuestCreateRequest;
import com.westbethel.motel_booking.guest.api.dto.GuestResponseDto;
import com.westbethel.motel_booking.guest.domain.Guest;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public ContactDetails toContactDetails(GuestCreateRequest.ContactDetailsDto dto) {
        if (dto == null) {
            return null;
        }
        return ContactDetails.builder()
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();
    }

    public Address toAddress(GuestCreateRequest.AddressDto dto) {
        if (dto == null) {
            return null;
        }
        return Address.builder()
                .line1(dto.getLine1())
                .line2(dto.getLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .build();
    }

    public GuestResponseDto toDto(Guest guest) {
        return GuestResponseDto.builder()
                .id(guest.getId())
                .customerNumber(guest.getCustomerNumber())
                .contactDetails(toContactDetailsDto(guest.getContactDetails()))
                .address(toAddressDto(guest.getAddress()))
                .preferences(guest.getPreferences())
                .marketingOptIn(guest.getMarketingOptIn())
                .loyaltyProfileId(guest.getLoyaltyProfileId())
                .build();
    }

    private GuestResponseDto.ContactDetailsDto toContactDetailsDto(ContactDetails contactDetails) {
        if (contactDetails == null) {
            return null;
        }
        return GuestResponseDto.ContactDetailsDto.builder()
                .email(contactDetails.getEmail())
                .phone(contactDetails.getPhone())
                .build();
    }

    private GuestResponseDto.AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return GuestResponseDto.AddressDto.builder()
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }
}
