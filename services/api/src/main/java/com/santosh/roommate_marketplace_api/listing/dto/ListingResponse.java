package com.santosh.roommate_marketplace_api.listing.dto;

import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id,
        Long ownerId,
        String ownerDisplayName,
        String title,
        String description,
        String city,
        String neighborhood,
        String universityName,
        BigDecimal monthlyRent,
        boolean utilitiesIncluded,
        LocalDate availableFrom,
        RoomType roomType,
        GenderPreference genderPreference,
        ListingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getOwner().getId(),
                buildOwnerDisplayName(listing),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCity(),
                listing.getNeighborhood(),
                listing.getUniversityName(),
                listing.getMonthlyRent(),
                listing.isUtilitiesIncluded(),
                listing.getAvailableFrom(),
                listing.getRoomType(),
                listing.getGenderPreference(),
                listing.getStatus(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }

    private static String buildOwnerDisplayName(Listing listing) {
        String firstName = listing.getOwner().getFirstName();
        String lastName = listing.getOwner().getLastName();

        if (firstName == null || firstName.isBlank()) {
            return null;
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.substring(0, 1) + ".";
    }
}
