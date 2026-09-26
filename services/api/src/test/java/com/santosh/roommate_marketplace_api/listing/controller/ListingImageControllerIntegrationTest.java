package com.santosh.roommate_marketplace_api.listing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingImage;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingImageRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class ListingImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void anyoneCanViewListingImagesSortedByDisplayOrder() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        createImage(listing, "https://example.com/second.jpg", 2);
        createImage(listing, "https://example.com/first.jpg", 1);

        mockMvc.perform(get("/api/listings/{listingId}/images", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].imageUrl").value("https://example.com/first.jpg"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1].imageUrl").value("https://example.com/second.jpg"));
    }

    @Test
    void ownerCanAddImage() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/images", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": "https://example.com/image.jpg",
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.getId()))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.displayOrder").value(0));
    }

    @Test
    void addImageRequiresValidUrlAndAuthentication() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/images", listing.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl": "https://example.com/image.jpg"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/listings/{listingId}/images", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl": "not-a-url"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotAddDeleteOrReorderImages() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("other@example.com", "Password123!");
        Listing listing = createListing(owner);
        ListingImage image = createImage(listing, "https://example.com/image.jpg", 0);
        String otherToken = loginAndExtractToken("other@example.com", "Password123!");

        mockMvc.perform(post("/api/listings/{listingId}/images", listing.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl": "https://example.com/new.jpg"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/listings/{listingId}/images/{imageId}", listing.getId(), image.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/listings/{listingId}/images/reorder", listing.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images": [
                                    {"imageId": %d, "displayOrder": 1}
                                  ]
                                }
                                """.formatted(image.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanDeleteImage() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        ListingImage image = createImage(listing, "https://example.com/image.jpg", 0);
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(delete("/api/listings/{listingId}/images/{imageId}", listing.getId(), image.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/listings/{listingId}/images", listing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ownerCanReorderImages() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        ListingImage first = createImage(listing, "https://example.com/first.jpg", 0);
        ListingImage second = createImage(listing, "https://example.com/second.jpg", 1);
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(patch("/api/listings/{listingId}/images/reorder", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "images": [
                                    {"imageId": %d, "displayOrder": 2},
                                    {"imageId": %d, "displayOrder": 1}
                                  ]
                                }
                                """.formatted(first.getId(), second.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1].id").value(first.getId()))
                .andExpect(jsonPath("$[1].displayOrder").value(2));
    }

    @Test
    void cannotAddMoreThanTenImages() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("owner@example.com", "Password123!");
        for (int index = 0; index < 10; index++) {
            createImage(listing, "https://example.com/" + index + ".jpg", index);
        }

        mockMvc.perform(post("/api/listings/{listingId}/images", listing.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageUrl": "https://example.com/too-many.jpg"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A listing can have at most 10 images"));
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
        user.setUserType(UserType.STUDENT);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test Owner");
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private Listing createListing(User owner) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room near campus");
        listing.setDescription("Clean private room close to university.");
        listing.setCity("Boston");
        listing.setNeighborhood("Fenway");
        listing.setUniversityName("Northeastern University");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setUtilitiesIncluded(true);
        listing.setAvailableFrom(LocalDate.of(2026, 9, 1));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setExactAddress("123 Private Street");
        return listingRepository.save(listing);
    }

    private ListingImage createImage(Listing listing, String imageUrl, int displayOrder) {
        ListingImage image = new ListingImage();
        image.setListing(listing);
        image.setImageUrl(imageUrl);
        image.setDisplayOrder(displayOrder);
        return listingImageRepository.save(image);
    }
}
