package com.santosh.roommate_marketplace_api.listing.repository;

import com.santosh.roommate_marketplace_api.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    int countByListingId(Long listingId);

    List<ListingImage> findByListingIdOrderByDisplayOrderAscIdAsc(Long listingId);

    Optional<ListingImage> findByIdAndListingId(Long id, Long listingId);
}
