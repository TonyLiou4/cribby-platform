package com.santosh.roommate_marketplace_api.favorite.controller;

import com.santosh.roommate_marketplace_api.favorite.dto.FavoriteStatusResponse;
import com.santosh.roommate_marketplace_api.favorite.dto.SavedListingResponse;
import com.santosh.roommate_marketplace_api.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Favorites", description = "Saved listings and favorite status endpoints")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/listings/{listingId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteStatusResponse favoriteListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        return favoriteService.favoriteListing(listingId, authentication.getName());
    }

    @DeleteMapping("/listings/{listingId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavoriteListing(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        favoriteService.unfavoriteListing(listingId, authentication.getName());
    }

    @GetMapping("/users/me/favorites")
    public List<SavedListingResponse> getMyFavorites(Authentication authentication) {
        return favoriteService.getMyFavorites(authentication.getName());
    }

    @GetMapping("/listings/{listingId}/favorite-status")
    public FavoriteStatusResponse getFavoriteStatus(
            @PathVariable Long listingId,
            Authentication authentication
    ) {
        return favoriteService.getFavoriteStatus(listingId, authentication.getName());
    }
}
