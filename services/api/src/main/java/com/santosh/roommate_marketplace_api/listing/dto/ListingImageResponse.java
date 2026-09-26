package com.santosh.roommate_marketplace_api.listing.dto;

import com.santosh.roommate_marketplace_api.listing.entity.ListingImage;

import java.time.LocalDateTime;

public record ListingImageResponse(
        Long id,
        Long listingId,
        String imageUrl,
        int displayOrder,
        LocalDateTime createdAt
) {
    public static ListingImageResponse from(ListingImage image) {
        return new ListingImageResponse(
                image.getId(),
                image.getListing().getId(),
                image.getImageUrl(),
                image.getDisplayOrder(),
                image.getCreatedAt()
        );
    }
}
