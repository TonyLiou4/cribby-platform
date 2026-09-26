package com.santosh.roommate_marketplace_api.favorite.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class FavoriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void favoriteEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/listings/1/favorite"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/listings/1/favorite"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me/favorites"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/listings/1/favorite-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanFavoriteAvailableListingAndCheckStatus() throws Exception {
        createUser("owner@example.com", "Password123!");
        User user = createUser("user@example.com", "Password123!");
        Listing listing = createListing(userRepository.findByEmailIgnoreCase("owner@example.com").orElseThrow(), ListingStatus.AVAILABLE);
        String token = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.getId()))
                .andExpect(jsonPath("$.favorited").value(true));

        mockMvc.perform(get("/api/listings/{listingId}/favorite-status", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true));

        org.assertj.core.api.Assertions.assertThat(
                favoriteRepository.existsByUserIdAndListingId(user.getId(), listing.getId())
        ).isTrue();
    }

    @Test
    void userCannotFavoriteOwnUnavailableOrDuplicateListing() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing ownListing = createListing(owner, ListingStatus.AVAILABLE);
        Listing pendingListing = createListing(owner, ListingStatus.PENDING);
        createUser("user@example.com", "Password123!");
        Listing availableListing = createListing(owner, ListingStatus.AVAILABLE);

        String ownerToken = loginAndExtractToken("owner@example.com", "Password123!");
        String userToken = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/favorite", ownListing.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot favorite your own listing"));

        mockMvc.perform(post("/api/listings/{listingId}/favorite", pendingListing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only available listings can be favorited"));

        mockMvc.perform(post("/api/listings/{listingId}/favorite", availableListing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/listings/{listingId}/favorite", availableListing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Listing is already favorited"));
    }

    @Test
    void unfavoriteIsIdempotent() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        User user = createUser("user@example.com", "Password123!");
        Listing listing = createListing(owner, ListingStatus.AVAILABLE);
        String token = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(delete("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(
                favoriteRepository.existsByUserIdAndListingId(user.getId(), listing.getId())
        ).isFalse();
    }

    @Test
    void getMyFavoritesReturnsSavedListingCardsWithoutFavoriteEntities() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("user@example.com", "Password123!");
        Listing listing = createListing(owner, ListingStatus.AVAILABLE);
        String token = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/favorite", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/me/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listingId").value(listing.getId()))
                .andExpect(jsonPath("$[0].title").value("Private room"))
                .andExpect(jsonPath("$[0].exactAddress").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].listing").doesNotExist());
    }

    @Test
    void favoriteStatusReturnsFalseAndMissingListingReturnsNotFound() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("user@example.com", "Password123!");
        Listing listing = createListing(owner, ListingStatus.AVAILABLE);
        String token = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(get("/api/listings/{listingId}/favorite-status", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));

        mockMvc.perform(get("/api/listings/999999/favorite-status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Listing not found"));
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private User createUser(String email, String password) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserType(UserType.STUDENT);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test User");
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private Listing createListing(User owner, ListingStatus status) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean private room close to university.");
        listing.setCity("Boston");
        listing.setNeighborhood("Fenway");
        listing.setUniversityName("Northeastern University");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setUtilitiesIncluded(true);
        listing.setAvailableFrom(LocalDate.of(2026, 9, 1));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(status);
        listing.setExactAddress("123 Private Street");
        return listingRepository.save(listing);
    }
}
