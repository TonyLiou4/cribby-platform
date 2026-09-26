package com.santosh.roommate_marketplace_api.favorite.dto;

import com.santosh.roommate_marketplace_api.favorite.entity.Favorite;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedListingResponse(
        Long favoriteId,
        Long listingId,
        Long ownerId,
        String title,
        String city,
        String neighborhood,
        String universityName,
        BigDecimal monthlyRent,
        boolean utilitiesIncluded,
        LocalDate availableFrom,
        RoomType roomType,
        GenderPreference genderPreference,
        ListingStatus status,
        LocalDateTime savedAt
) {
    public static SavedListingResponse from(Favorite favorite) {
        Listing listing = favorite.getListing();
        return new SavedListingResponse(
                favorite.getId(),
                listing.getId(),
                listing.getOwner().getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getNeighborhood(),
                listing.getUniversityName(),
                listing.getMonthlyRent(),
                listing.isUtilitiesIncluded(),
                listing.getAvailableFrom(),
                listing.getRoomType(),
                listing.getGenderPreference(),
                listing.getStatus(),
                favorite.getCreatedAt()
        );
    }
}
