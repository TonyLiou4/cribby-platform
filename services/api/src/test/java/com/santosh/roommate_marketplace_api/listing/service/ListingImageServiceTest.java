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
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingImageServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @InjectMocks
    private ListingImageService listingImageService;

    @Test
    void ownerCanAddImage() {
        Listing listing = listing("owner@example.com");
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingImageRepository.countByListingId(1L)).thenReturn(2);
        when(listingImageRepository.save(any(ListingImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingImageResponse response = listingImageService.addImage(
                1L,
                "owner@example.com",
                new ListingImageRequest("https://example.com/image.jpg", null)
        );

        assertThat(response.imageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(response.displayOrder()).isEqualTo(2);
        verify(listingImageRepository).save(any(ListingImage.class));
    }

    @Test
    void nonOwnerCannotAddImage() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing("owner@example.com")));

        assertThatThrownBy(() -> listingImageService.addImage(
                1L,
                "other@example.com",
                new ListingImageRequest("https://example.com/image.jpg", 0)
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotAddMoreThanTenImages() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing("owner@example.com")));
        when(listingImageRepository.countByListingId(1L)).thenReturn(10);

        assertThatThrownBy(() -> listingImageService.addImage(
                1L,
                "owner@example.com",
                new ListingImageRequest("https://example.com/image.jpg", 0)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getImagesReturnsSortedRepositoryResults() {
        when(listingRepository.existsById(1L)).thenReturn(true);
        when(listingImageRepository.findByListingIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(image(1L, 1), image(2L, 2)));

        List<ListingImageResponse> response = listingImageService.getImages(1L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).displayOrder()).isEqualTo(1);
    }

    @Test
    void ownerCanDeleteImage() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing("owner@example.com")));
        ListingImage image = image(5L, 0);
        when(listingImageRepository.findByIdAndListingId(5L, 1L)).thenReturn(Optional.of(image));

        listingImageService.deleteImage(1L, 5L, "owner@example.com");

        verify(listingImageRepository).delete(image);
    }

    @Test
    void reorderRejectsImageFromAnotherListing() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing("owner@example.com")));
        when(listingImageRepository.findByListingIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(image(1L, 0)));

        assertThatThrownBy(() -> listingImageService.reorderImages(
                1L,
                "owner@example.com",
                new ListingImageReorderRequest(List.of(new ListingImageReorderItem(99L, 0)))
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    private Listing listing(String ownerEmail) {
        User owner = new User();
        owner.setEmail(ownerEmail);
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setPasswordHash("hashed-password");
        owner.setUserType(UserType.STUDENT);

        Listing listing = new Listing();
        listing.setOwner(owner);
        return listing;
    }

    private ListingImage image(Long id, int displayOrder) {
        Listing listing = listing("owner@example.com");
        ListingImage image = new ListingImage();
        image.setListing(listing);
        image.setImageUrl("https://example.com/" + id + ".jpg");
        image.setDisplayOrder(displayOrder);
        return image;
    }
}
