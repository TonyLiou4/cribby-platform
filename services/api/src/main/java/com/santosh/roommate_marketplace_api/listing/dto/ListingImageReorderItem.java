package com.santosh.roommate_marketplace_api.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ListingImageReorderItem(
        @NotNull
        Long imageId,

        @Min(0)
        int displayOrder
) {
}
