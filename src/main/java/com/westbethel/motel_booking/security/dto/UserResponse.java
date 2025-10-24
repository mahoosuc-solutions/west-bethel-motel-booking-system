package com.westbethel.motel_booking.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user information responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private Boolean enabled;

    private List<String> roles;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;
}
