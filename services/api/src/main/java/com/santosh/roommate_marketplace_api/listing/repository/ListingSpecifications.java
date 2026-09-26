package com.santosh.roommate_marketplace_api.listing.repository;

import com.santosh.roommate_marketplace_api.listing.dto.ListingSearchCriteria;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> publicSearch(ListingSearchCriteria criteria) {
        return Specification
                .where(hasStatus(ListingStatus.AVAILABLE))
                .and(containsIgnoreCase("city", criteria.city()))
                .and(containsIgnoreCase("universityName", criteria.universityName()))
                .and(minRent(criteria.minRent()))
                .and(maxRent(criteria.maxRent()))
                .and(criteria.roomType() == null ? null : (root, query, cb) -> cb.equal(root.get("roomType"), criteria.roomType()))
                .and(criteria.genderPreference() == null ? null : (root, query, cb) -> cb.equal(root.get("genderPreference"), criteria.genderPreference()))
                .and(availableFrom(criteria.availableFrom()))
                .and(keyword(criteria.keyword()))
                .and(excludeOwner(criteria.excludedOwnerId()));
    }

    private static Specification<Listing> hasStatus(ListingStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Listing> containsIgnoreCase(String field, String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), "%" + normalizedValue + "%");
    }

    private static Specification<Listing> minRent(BigDecimal minRent) {
        if (minRent == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("monthlyRent"), minRent);
    }

    private static Specification<Listing> maxRent(BigDecimal maxRent) {
        if (maxRent == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("monthlyRent"), maxRent);
    }

    private static Specification<Listing> availableFrom(LocalDate availableFrom) {
        if (availableFrom == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("availableFrom"), availableFrom);
    }

    private static Specification<Listing> keyword(String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword == null) {
            return null;
        }

        String pattern = "%" + normalizedKeyword + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("city")), pattern),
                cb.like(cb.lower(root.get("neighborhood")), pattern),
                cb.like(cb.lower(root.get("universityName")), pattern)
        );
    }

    private static Specification<Listing> excludeOwner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.notEqual(root.get("owner").get("id"), ownerId);
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
