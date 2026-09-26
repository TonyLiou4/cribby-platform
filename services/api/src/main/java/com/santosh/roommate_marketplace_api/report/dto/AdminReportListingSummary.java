package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.listing.entity.Listing;

public record AdminReportListingSummary(
        Long id,
        String title,
        String city,
        String universityName
) {
    public static AdminReportListingSummary from(Listing listing) {
        if (listing == null) {
            return null;
        }

        return new AdminReportListingSummary(
                listing.getId(),
                listing.getTitle(),
                listing.getCity(),
                listing.getUniversityName()
        );
    }
}
