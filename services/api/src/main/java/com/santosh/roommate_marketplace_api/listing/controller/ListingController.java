package com.santosh.roommate_marketplace_api.listing.controller;

import com.santosh.roommate_marketplace_api.common.dto.PagedResponse;
import com.santosh.roommate_marketplace_api.listing.dto.ListingRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingResponse;
import com.santosh.roommate_marketplace_api.listing.dto.ListingSearchCriteria;
import com.santosh.roommate_marketplace_api.listing.dto.ListingStatusUpdateRequest;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.service.ListingService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
@Tag(name = "Listings", description = "Listing CRUD, search, filters, sorting, and pagination")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse createListing(
            Authentication authentication,
            @Valid @RequestBody ListingRequest request
    ) {
        return listingService.createListing(authentication.getName(), request);
    }

    @GetMapping
    public PagedResponse<ListingResponse> getListings(
            Authentication authentication,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String universityName,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) GenderPreference genderPreference,
            @RequestParam(required = false) LocalDate availableFrom,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @Parameter(description = "When true and authenticated, exclude listings owned by the current user.")
            @RequestParam(defaultValue = "false") boolean excludeOwn
    ) {
        Long excludedOwnerId = excludeOwn && isAuthenticatedUser(authentication)
                ? listingService.getUserIdByEmail(authentication.getName())
                : null;
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                city,
                universityName,
                maxRent,
                minRent,
                roomType,
                genderPreference,
                availableFrom,
                keyword,
                excludedOwnerId
        );
        return listingService.searchAvailableListings(criteria, page, size, sort);
    }

    private boolean isAuthenticatedUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @GetMapping("/me")
    public List<ListingResponse> getMyListings(Authentication authentication) {
        return listingService.getMyListings(authentication.getName());
    }

    @GetMapping("/{id}")
    public ListingResponse getListing(@PathVariable Long id) {
        return listingService.getPublicListing(id);
    }

    @PutMapping("/{id}")
    public ListingResponse updateListing(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ListingRequest request
    ) {
        return listingService.updateListing(id, authentication.getName(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(@PathVariable Long id, Authentication authentication) {
        listingService.deleteListing(id, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    public ListingResponse updateStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ListingStatusUpdateRequest request
    ) {
        return listingService.updateStatus(id, authentication.getName(), request.status());
    }
}
