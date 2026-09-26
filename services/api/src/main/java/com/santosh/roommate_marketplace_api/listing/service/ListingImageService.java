package com.santosh.roommate_marketplace_api.listing.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageReorderItem;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageReorderRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingImageResponse;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingImage;
import com.santosh.roommate_marketplace_api.listing.repository.ListingImageRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListingImageService {

    private static final int MAX_IMAGES_PER_LISTING = 10;

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;

    public ListingImageService(
            ListingRepository listingRepository,
            ListingImageRepository listingImageRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingImageRepository = listingImageRepository;
    }

    @Transactional
    public ListingImageResponse addImage(Long listingId, String ownerEmail, ListingImageRequest request) {
        Listing listing = getListingForOwner(listingId, ownerEmail);
        if (listingImageRepository.countByListingId(listingId) >= MAX_IMAGES_PER_LISTING) {
            throw new BadRequestException("A listing can have at most 10 images");
        }

        ListingImage image = new ListingImage();
        image.setListing(listing);
        image.setImageUrl(request.imageUrl().trim());
        image.setDisplayOrder(resolveDisplayOrder(listingId, request.displayOrder()));

        return ListingImageResponse.from(listingImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<ListingImageResponse> getImages(Long listingId) {
        ensureListingExists(listingId);
        return listingImageRepository.findByListingIdOrderByDisplayOrderAscIdAsc(listingId)
                .stream()
                .map(ListingImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(Long listingId, Long imageId, String ownerEmail) {
        getListingForOwner(listingId, ownerEmail);
        ListingImage image = listingImageRepository.findByIdAndListingId(imageId, listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing image not found"));

        listingImageRepository.delete(image);
    }

    @Transactional
    public List<ListingImageResponse> reorderImages(
            Long listingId,
            String ownerEmail,
            ListingImageReorderRequest request
    ) {
        getListingForOwner(listingId, ownerEmail);
        List<ListingImage> existingImages = listingImageRepository.findByListingIdOrderByDisplayOrderAscIdAsc(listingId);
        Map<Long, ListingImage> imagesById = existingImages.stream()
                .collect(Collectors.toMap(ListingImage::getId, Function.identity()));

        Set<Long> seenIds = new HashSet<>();
        for (ListingImageReorderItem item : request.images()) {
            if (!seenIds.add(item.imageId())) {
                throw new BadRequestException("Duplicate image id in reorder request");
            }

            ListingImage image = imagesById.get(item.imageId());
            if (image == null) {
                throw new ResourceNotFoundException("Listing image not found");
            }
            image.setDisplayOrder(item.displayOrder());
        }

        return existingImages.stream()
                .sorted((first, second) -> {
                    int orderComparison = Integer.compare(first.getDisplayOrder(), second.getDisplayOrder());
                    if (orderComparison != 0) {
                        return orderComparison;
                    }
                    return first.getId().compareTo(second.getId());
                })
                .map(ListingImageResponse::from)
                .toList();
    }

    private int resolveDisplayOrder(Long listingId, Integer requestedDisplayOrder) {
        if (requestedDisplayOrder != null) {
            return requestedDisplayOrder;
        }
        return listingImageRepository.countByListingId(listingId);
    }

    private Listing getListingForOwner(Long listingId, String ownerEmail) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new AccessDeniedException("Only the listing owner can modify listing images");
        }
        return listing;
    }

    private void ensureListingExists(Long listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing not found");
        }
    }
}
