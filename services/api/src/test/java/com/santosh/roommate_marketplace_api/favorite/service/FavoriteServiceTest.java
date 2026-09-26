package com.santosh.roommate_marketplace_api.favorite.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.favorite.dto.FavoriteStatusResponse;
import com.santosh.roommate_marketplace_api.favorite.dto.SavedListingResponse;
import com.santosh.roommate_marketplace_api.favorite.entity.Favorite;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void userCanFavoriteAvailableListing() {
        User user = user(1L, "user@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"), ListingStatus.AVAILABLE);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(false);

        FavoriteStatusResponse response = favoriteService.favoriteListing(10L, "user@example.com");

        assertThat(response.favorited()).isTrue();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void userCannotFavoriteOwnListing() {
        User owner = user(1L, "owner@example.com");
        Listing listing = listing(10L, owner, ListingStatus.AVAILABLE);

        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> favoriteService.favoriteListing(10L, "owner@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own listing");
    }

    @Test
    void userCannotFavoriteUnavailableListing() {
        User user = user(1L, "user@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"), ListingStatus.PENDING);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> favoriteService.favoriteListing(10L, "user@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("available");
    }

    @Test
    void userCannotFavoriteSameListingTwice() {
        User user = user(1L, "user@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"), ListingStatus.AVAILABLE);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserIdAndListingId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.favoriteListing(10L, "user@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already favorited");
    }

    @Test
    void unfavoriteIsIdempotentWhenFavoriteDoesNotExist() {
        User user = user(1L, "user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndListingId(1L, 10L)).thenReturn(Optional.empty());

        favoriteService.unfavoriteListing(10L, "user@example.com");

        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    void getMyFavoritesReturnsSavedListingDtos() {
        User user = user(1L, "user@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"), ListingStatus.AVAILABLE);
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setListing(listing);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(favorite));

        List<SavedListingResponse> response = favoriteService.getMyFavorites("user@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).listingId()).isEqualTo(10L);
        assertThat(response.get(0).title()).isEqualTo("Private room");
    }

    @Test
    void favoriteStatusReturnsNotFoundForMissingListing() {
        User user = user(1L, "user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(listingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.getFavoriteStatus(99L, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, id);
        return user;
    }

    private Listing listing(Long id, User owner, ListingStatus status) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean room.");
        listing.setCity("Boston");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(status);
        setId(listing, id);
        return listing;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = com.santosh.roommate_marketplace_api.common.entity.BaseEntity.class
                    .getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
