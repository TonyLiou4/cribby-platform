package com.santosh.roommate_marketplace_api.favorite.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.favorite.dto.FavoriteStatusResponse;
import com.santosh.roommate_marketplace_api.favorite.dto.SavedListingResponse;
import com.santosh.roommate_marketplace_api.favorite.entity.Favorite;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            ListingRepository listingRepository,
            UserRepository userRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FavoriteStatusResponse favoriteListing(Long listingId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Listing listing = getListing(listingId);

        if (listing.getStatus() != ListingStatus.AVAILABLE) {
            throw new BadRequestException("Only available listings can be favorited");
        }
        if (listing.getOwner().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot favorite your own listing");
        }
        if (favoriteRepository.existsByUserIdAndListingId(user.getId(), listingId)) {
            throw new BadRequestException("Listing is already favorited");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setListing(listing);
        favoriteRepository.save(favorite);

        return new FavoriteStatusResponse(listingId, true);
    }

    @Transactional
    public void unfavoriteListing(Long listingId, String userEmail) {
        User user = getUserByEmail(userEmail);
        favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<SavedListingResponse> getMyFavorites(String userEmail) {
        User user = getUserByEmail(userEmail);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(SavedListingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse getFavoriteStatus(Long listingId, String userEmail) {
        User user = getUserByEmail(userEmail);
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing not found");
        }

        boolean favorited = favoriteRepository.existsByUserIdAndListingId(user.getId(), listingId);
        return new FavoriteStatusResponse(listingId, favorited);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
    }
}
