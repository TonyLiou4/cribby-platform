package com.santosh.roommate_marketplace_api.listing.repository;

import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Listing> findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(String ownerEmail);
}
