package com.santosh.roommate_marketplace_api.listing.dto;

import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import jakarta.validation.constraints.NotNull;

public record ListingStatusUpdateRequest(
        @NotNull
        ListingStatus status
) {
}
