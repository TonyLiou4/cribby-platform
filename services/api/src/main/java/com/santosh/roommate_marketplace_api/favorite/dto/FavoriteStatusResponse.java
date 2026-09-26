package com.santosh.roommate_marketplace_api.favorite.dto;

public record FavoriteStatusResponse(
        Long listingId,
        boolean favorited
) {
}
