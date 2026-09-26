package com.santosh.roommate_marketplace_api.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ListingImageRequest(
        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://")
        String imageUrl,

        @Min(0)
        Integer displayOrder
) {
}
