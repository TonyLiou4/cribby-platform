package com.santosh.roommate_marketplace_api.listing.dto;

import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListingSearchCriteria(
        String city,
        String universityName,
        BigDecimal maxRent,
        BigDecimal minRent,
        RoomType roomType,
        GenderPreference genderPreference,
        LocalDate availableFrom,
        String keyword,
        Long excludedOwnerId
) {
}
