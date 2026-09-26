package com.santosh.roommate_marketplace_api.listing.service;

import com.santosh.roommate_marketplace_api.common.dto.PagedResponse;
import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.dto.ListingRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingResponse;
import com.santosh.roommate_marketplace_api.listing.dto.ListingSearchCriteria;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingSpecifications;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ListingService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "monthlyRent",
            "availableFrom",
            "title",
            "city"
    );

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ListingResponse createListing(String ownerEmail, ListingRequest request) {
        User owner = getUserByEmail(ownerEmail);

        Listing listing = new Listing();
        listing.setOwner(owner);
        applyRequest(listing, request);
        listing.setStatus(ListingStatus.AVAILABLE);

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        return getUserByEmail(email).getId();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ListingResponse> searchAvailableListings(
            ListingSearchCriteria criteria,
            Integer page,
            Integer size,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<ListingResponse> listings = listingRepository
                .findAll(ListingSpecifications.publicSearch(criteria), pageable)
                .map(ListingResponse::from);
        return PagedResponse.from(listings);
    }

    @Transactional(readOnly = true)
    public ListingResponse getPublicListing(Long id) {
        Listing listing = listingRepository.findById(id)
                .filter(foundListing -> foundListing.getStatus() == ListingStatus.AVAILABLE)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        return ListingResponse.from(listing);
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings(String ownerEmail) {
        getUserByEmail(ownerEmail);
        return listingRepository.findByOwnerEmailIgnoreCaseOrderByCreatedAtDesc(ownerEmail)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    @Transactional
    public ListingResponse updateListing(Long id, String ownerEmail, ListingRequest request) {
        Listing listing = getListingForOwner(id, ownerEmail);
        applyRequest(listing, request);
        return ListingResponse.from(listing);
    }

    @Transactional
    public void deleteListing(Long id, String ownerEmail) {
        Listing listing = getListingForOwner(id, ownerEmail);
        listingRepository.delete(listing);
    }

    @Transactional
    public ListingResponse updateStatus(Long id, String ownerEmail, ListingStatus status) {
        Listing listing = getListingForOwner(id, ownerEmail);
        listing.setStatus(status);
        return ListingResponse.from(listing);
    }

    private Listing getListingForOwner(Long id, String ownerEmail) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new AccessDeniedException("Only the listing owner can modify this listing");
        }

        return listing;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Pageable buildPageable(Integer page, Integer size, String sort) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (pageNumber < 0) {
            throw new BadRequestException("Page must be greater than or equal to 0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and 50");
        }

        return PageRequest.of(pageNumber, pageSize, parseSort(sort));
    }

    private Sort parseSort(String sort) {
        String sortValue = sort == null || sort.isBlank() ? "createdAt,desc" : sort.trim();
        String[] parts = sortValue.split(",");
        String field = parts[0].trim();
        String directionValue = parts.length > 1 ? parts[1].trim() : "asc";

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BadRequestException("Unsupported sort field: " + field);
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(directionValue);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported sort direction: " + directionValue);
        }

        return Sort.by(direction, field);
    }

    private void applyRequest(Listing listing, ListingRequest request) {
        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setCity(request.city().trim());
        listing.setNeighborhood(trimToNull(request.neighborhood()));
        listing.setUniversityName(trimToNull(request.universityName()));
        listing.setMonthlyRent(request.monthlyRent());
        listing.setUtilitiesIncluded(request.utilitiesIncluded());
        listing.setAvailableFrom(request.availableFrom());
        listing.setRoomType(request.roomType());
        listing.setGenderPreference(request.genderPreference());
        listing.setExactAddress(trimToNull(request.exactAddress()));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
