package com.santosh.roommate_marketplace_api.listing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ListingImageReorderRequest(
        @NotEmpty
        @Size(max = 10)
        List<@Valid ListingImageReorderItem> images
) {
}
