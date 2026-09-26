package com.santosh.roommate_marketplace_api.listing.controller;

import com.santosh.roommate_marketplace_api.listing.dto.ListingImageReorderRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageResponse;
import com.santosh.roommate_marketplace_api.listing.service.ListingImageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
@Tag(name = "Listing Images", description = "Image URL management for listings")
public class ListingImageController {

    private final ListingImageService listingImageService;

    public ListingImageController(ListingImageService listingImageService) {
        this.listingImageService = listingImageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingImageResponse addImage(
            @PathVariable Long listingId,
            Authentication authentication,
            @Valid @RequestBody ListingImageRequest request
    ) {
        return listingImageService.addImage(listingId, authentication.getName(), request);
    }

    @GetMapping
    public List<ListingImageResponse> getImages(@PathVariable Long listingId) {
        return listingImageService.getImages(listingId);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
            @PathVariable Long listingId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        listingImageService.deleteImage(listingId, imageId, authentication.getName());
    }

    @PatchMapping("/reorder")
    public List<ListingImageResponse> reorderImages(
            @PathVariable Long listingId,
            Authentication authentication,
            @Valid @RequestBody ListingImageReorderRequest request
    ) {
        return listingImageService.reorderImages(listingId, authentication.getName(), request);
    }
}
