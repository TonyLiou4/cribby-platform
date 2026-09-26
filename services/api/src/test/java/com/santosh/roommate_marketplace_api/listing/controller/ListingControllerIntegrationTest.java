package com.santosh.roommate_marketplace_api.listing.controller;

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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class ListingControllerIntegrationTest {

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
    void createListingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validListingJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateListing() throws Exception {
        createUser("owner@example.com", "Password123!");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validListingJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Private room near campus"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.exactAddress").doesNotExist());
    }

    @Test
    void publicBrowsingShowsOnlyAvailableListings() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createListing(owner, ListingStatus.AVAILABLE, "Available listing");
        createListing(owner, ListingStatus.PENDING, "Pending listing");

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Available listing"))
                .andExpect(jsonPath("$.content[0].exactAddress").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void unauthenticatedPublicBrowsingWithExcludeOwnStillWorksNormally() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createListing(owner, ListingStatus.AVAILABLE, "Available listing");

        mockMvc.perform(get("/api/listings")
                        .param("excludeOwn", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Available listing"));
    }

    @Test
    void authenticatedPublicBrowsingCanExcludeOwnListings() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        User other = createUser("other@example.com", "Password123!");
        createListing(owner, ListingStatus.AVAILABLE, "Owner available listing");
        createListing(other, ListingStatus.AVAILABLE, "Other available listing");
        createListing(other, ListingStatus.PENDING, "Other pending listing");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(get("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .param("excludeOwn", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Other available listing"))
                .andExpect(jsonPath("$.content[0].exactAddress").doesNotExist());
    }

    @Test
    void authenticatedPublicBrowsingWithExcludeOwnFalseIncludesOwnListings() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        User other = createUser("other@example.com", "Password123!");
        createListing(owner, ListingStatus.AVAILABLE, "Owner available listing");
        createListing(other, ListingStatus.AVAILABLE, "Other available listing");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(get("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .param("excludeOwn", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title", org.hamcrest.Matchers.containsInAnyOrder(
                        "Owner available listing",
                        "Other available listing"
                )));
    }

    @Test
    void publicSearchSupportsFiltersSortingAndPagination() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing bostonPrivate = createListing(owner, ListingStatus.AVAILABLE, "Sunny private room");
        bostonPrivate.setCity("Boston");
        bostonPrivate.setNeighborhood("Fenway");
        bostonPrivate.setUniversityName("Northeastern University");
        bostonPrivate.setMonthlyRent(new BigDecimal("1200.00"));
        bostonPrivate.setRoomType(RoomType.PRIVATE_ROOM);
        bostonPrivate.setGenderPreference(GenderPreference.ANY);
        listingRepository.save(bostonPrivate);

        Listing cambridgeShared = createListing(owner, ListingStatus.AVAILABLE, "Shared room near Harvard");
        cambridgeShared.setCity("Cambridge");
        cambridgeShared.setNeighborhood("Harvard Square");
        cambridgeShared.setUniversityName("Harvard University");
        cambridgeShared.setMonthlyRent(new BigDecimal("900.00"));
        cambridgeShared.setRoomType(RoomType.SHARED_ROOM);
        cambridgeShared.setGenderPreference(GenderPreference.FEMALE);
        listingRepository.save(cambridgeShared);

        Listing pending = createListing(owner, ListingStatus.PENDING, "Boston private pending");
        pending.setCity("Boston");
        pending.setMonthlyRent(new BigDecimal("800.00"));
        listingRepository.save(pending);

        mockMvc.perform(get("/api/listings")
                        .param("city", "Boston")
                        .param("universityName", "Northeastern")
                        .param("minRent", "1000")
                        .param("maxRent", "1300")
                        .param("roomType", "PRIVATE_ROOM")
                        .param("genderPreference", "ANY")
                        .param("availableFrom", "2026-09-15")
                        .param("keyword", "sunny")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "monthlyRent,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Sunny private room"))
                .andExpect(jsonPath("$.content[0].exactAddress").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void publicSearchRejectsInvalidPaginationAndSort() throws Exception {
        mockMvc.perform(get("/api/listings")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 50"));

        mockMvc.perform(get("/api/listings")
                        .param("sort", "exactAddress,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort field: exactAddress"));
    }

    @Test
    void publicGetListingReturnsAvailableListingOnly() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing available = createListing(owner, ListingStatus.AVAILABLE, "Available listing");
        Listing pending = createListing(owner, ListingStatus.PENDING, "Pending listing");

        mockMvc.perform(get("/api/listings/{id}", available.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Available listing"))
                .andExpect(jsonPath("$.exactAddress").doesNotExist());

        mockMvc.perform(get("/api/listings/{id}", pending.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyListingsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/listings/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyListingsReturnsOnlyAuthenticatedOwnersListings() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        User other = createUser("other@example.com", "Password123!");
        createListing(owner, ListingStatus.AVAILABLE, "Owner available listing");
        createListing(owner, ListingStatus.PENDING, "Owner pending listing");
        createListing(other, ListingStatus.AVAILABLE, "Other available listing");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(get("/api/listings/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", org.hamcrest.Matchers.containsInAnyOrder(
                        "Owner available listing",
                        "Owner pending listing"
                )));
    }

    @Test
    void ownerCanUpdateDeleteAndChangeStatus() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner, ListingStatus.AVAILABLE, "Original listing");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(put("/api/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validListingJson().replace("Private room near campus", "Updated listing")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated listing"));

        mockMvc.perform(patch("/api/listings/{id}/status", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OCCUPIED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED"));

        mockMvc.perform(delete("/api/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonOwnerCannotModifyListing() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("other@example.com", "Password123!");
        Listing listing = createListing(owner, ListingStatus.AVAILABLE, "Owner listing");
        String otherToken = loginAndExtractToken("other@example.com", "Password123!");

        mockMvc.perform(put("/api/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validListingJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/listings/{id}/status", listing.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OCCUPIED"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/listings/{id}", listing.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCreateRequestFailsValidation() throws Exception {
        createUser("owner@example.com", "Password123!");
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Valid description",
                                  "city": "Boston",
                                  "monthlyRent": -1,
                                  "roomType": "PRIVATE_ROOM",
                                  "genderPreference": "ANY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    private String validListingJson() {
        return """
                {
                  "title": "Private room near campus",
                  "description": "Clean private room close to university.",
                  "city": "Boston",
                  "neighborhood": "Fenway",
                  "universityName": "Northeastern University",
                  "monthlyRent": 1200.00,
                  "utilitiesIncluded": true,
                  "availableFrom": "2026-09-01",
                  "roomType": "PRIVATE_ROOM",
                  "genderPreference": "ANY",
                  "exactAddress": "123 Private Street"
                }
                """;
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
        user.setLastName("Owner");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUniversityName("Northeastern University");
        user.setUserType(UserType.STUDENT);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test Owner");
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private Listing createListing(User owner, ListingStatus status, String title) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle(title);
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
