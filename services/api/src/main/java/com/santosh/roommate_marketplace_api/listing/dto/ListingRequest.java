package com.santosh.roommate_marketplace_api.listing.dto;

import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListingRequest(
        @NotBlank
        @Size(max = 160)
        String title,

        @NotBlank
        @Size(max = 3000)
        String description,

        @NotBlank
        @Size(max = 120)
        String city,

        @Size(max = 120)
        String neighborhood,

        @Size(max = 160)
        String universityName,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal monthlyRent,

        boolean utilitiesIncluded,

        LocalDate availableFrom,

        @NotNull
        RoomType roomType,

        @NotNull
        GenderPreference genderPreference,

        @Size(max = 500)
        String exactAddress
) {
}
