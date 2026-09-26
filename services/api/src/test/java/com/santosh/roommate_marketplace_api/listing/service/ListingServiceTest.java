package com.santosh.roommate_marketplace_api.listing.service;

import com.santosh.roommate_marketplace_api.common.dto.PagedResponse;
import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.dto.ListingRequest;
import com.santosh.roommate_marketplace_api.listing.dto.ListingResponse;
import com.santosh.roommate_marketplace_api.listing.dto.ListingSearchCriteria;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService listingService;

    @Test
    void createListingAssignsAuthenticatedOwnerAndAvailableStatus() {
        User owner = user("owner@example.com");
        ListingRequest request = validRequest("Private room");

        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListingResponse response = listingService.createListing("owner@example.com", request);

        assertThat(response.title()).isEqualTo("Private room");
        assertThat(response.status()).isEqualTo(ListingStatus.AVAILABLE);
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void searchAvailableListingsReturnsPagedAvailableListings() {
        Listing listing = listing(user("owner@example.com"), ListingStatus.AVAILABLE);
        when(listingRepository.findAll(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(listing)));

        PagedResponse<ListingResponse> response = listingService.searchAvailableListings(
                emptyCriteria(),
                0,
                10,
                "createdAt,desc"
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo(ListingStatus.AVAILABLE);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
    }

    @Test
    void searchRejectsInvalidPageSize() {
        assertThatThrownBy(() -> listingService.searchAvailableListings(emptyCriteria(), 0, 51, "createdAt,desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Size must be between 1 and 50");
    }

    @Test
    void searchRejectsUnsupportedSortField() {
        assertThatThrownBy(() -> listingService.searchAvailableListings(emptyCriteria(), 0, 10, "exactAddress,desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    @Test
    void publicListingMissingOrUnavailableReturnsNotFound() {
        Listing pendingListing = listing(user("owner@example.com"), ListingStatus.PENDING);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(pendingListing));

        assertThatThrownBy(() -> listingService.getPublicListing(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownerCanUpdateListing() {
        Listing listing = listing(user("owner@example.com"), ListingStatus.AVAILABLE);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        ListingResponse response = listingService.updateListing(1L, "owner@example.com", validRequest("Updated room"));

        assertThat(response.title()).isEqualTo("Updated room");
        assertThat(listing.getTitle()).isEqualTo("Updated room");
    }

    @Test
    void nonOwnerCannotUpdateListing() {
        Listing listing = listing(user("owner@example.com"), ListingStatus.AVAILABLE);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.updateListing(1L, "other@example.com", validRequest("Updated room")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerCanChangeStatus() {
        Listing listing = listing(user("owner@example.com"), ListingStatus.AVAILABLE);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        ListingResponse response = listingService.updateStatus(1L, "owner@example.com", ListingStatus.OCCUPIED);

        assertThat(response.status()).isEqualTo(ListingStatus.OCCUPIED);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.OCCUPIED);
    }

    private ListingRequest validRequest(String title) {
        return new ListingRequest(
                title,
                "Clean room near campus.",
                "Boston",
                "Fenway",
                "Northeastern University",
                new BigDecimal("1200.00"),
                true,
                LocalDate.now().plusDays(30),
                RoomType.PRIVATE_ROOM,
                GenderPreference.ANY,
                "123 Private Street"
        );
    }

    private ListingSearchCriteria emptyCriteria() {
        return new ListingSearchCriteria(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Listing listing(User owner, ListingStatus status) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean room near campus.");
        listing.setCity("Boston");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(status);
        return listing;
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Owner");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        return user;
    }
}
