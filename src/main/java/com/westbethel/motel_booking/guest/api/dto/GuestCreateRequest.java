package com.westbethel.motel_booking.guest.api.dto;

import com.westbethel.motel_booking.common.validation.NoSpecialCharacters;
import com.westbethel.motel_booking.common.validation.ValidPhoneNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestCreateRequest {

    @Valid
    @NotNull(message = "Contact details are required")
    private ContactDetailsDto contactDetails;

    @Valid
    private AddressDto address;

    @Size(max = 1000, message = "Preferences cannot exceed 1000 characters")
    @NoSpecialCharacters(mode = NoSpecialCharacters.Mode.RELAXED)
    private String preferences;

    @NotNull(message = "Marketing opt-in preference is required")
    private Boolean marketingOptIn;

    @Getter
    @Setter
    public static class ContactDetailsDto {

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid", regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        private String email;

        @ValidPhoneNumber(required = true)
        private String phone;
    }

    @Getter
    @Setter
    public static class AddressDto {

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 200, message = "Address line 1 cannot exceed 200 characters")
        @NoSpecialCharacters(mode = NoSpecialCharacters.Mode.RELAXED)
        private String line1;

        @Size(max = 200, message = "Address line 2 cannot exceed 200 characters")
        @NoSpecialCharacters(mode = NoSpecialCharacters.Mode.RELAXED)
        private String line2;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        @NoSpecialCharacters
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        @NoSpecialCharacters
        private String state;

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        @Pattern(regexp = "^[A-Za-z0-9\\s-]+$", message = "Postal code contains invalid characters")
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        @Pattern(regexp = "^[A-Za-z\\s-]+$", message = "Country name can only contain letters, spaces, and hyphens")
        private String country;
    }
}
