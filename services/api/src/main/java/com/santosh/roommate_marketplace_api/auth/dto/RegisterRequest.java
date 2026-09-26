package com.santosh.roommate_marketplace_api.auth.dto;

import com.santosh.roommate_marketplace_api.user.entity.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        String firstName,

        @NotBlank
        @Size(min = 2, max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Pattern(regexp = "^[+0-9 .()-]{7,30}$", message = "must be a valid phone number")
        @Size(max = 30)
        String phoneNumber,

        @Size(max = 160)
        String universityName,

        @Size(max = 1000)
        String bio,

        @Size(max = 40)
        String gender,

        UserType userType
) {
}
