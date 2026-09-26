package com.santosh.roommate_marketplace_api.user.dto;

import com.santosh.roommate_marketplace_api.user.entity.UserType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100)
        String firstName,

        @Size(min = 2, max = 100)
        String lastName,

        @Pattern(regexp = "^[+0-9 .()-]{7,30}$", message = "must be a valid phone number")
        @Size(max = 30)
        String phoneNumber,

        @Size(max = 160)
        String universityName,

        @Size(max = 500)
        String profilePhotoUrl,

        @Size(max = 1000)
        String bio,

        @Size(max = 40)
        String gender,

        UserType userType
) {
}
