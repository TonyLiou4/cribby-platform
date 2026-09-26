package com.santosh.roommate_marketplace_api.report.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingImageRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class ReportControllerIntegrationTest {

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
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        favoriteRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void reportEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "USER",
                                  "reportedUserId": 1,
                                  "reason": "This user is asking for payment outside the platform."
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReportListing() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("reporter@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("reporter@example.com", "Password123!");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "LISTING",
                                  "reportedListingId": %d,
                                  "reason": "This listing looks suspicious and uses copied photos."
                                }
                                """.formatted(listing.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportType").value("LISTING"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reportedListingId").value(listing.getId()));
    }

    @Test
    void authenticatedUserCanReportAnotherUser() throws Exception {
        User reportedUser = createUser("reported@example.com", "Password123!");
        createUser("reporter@example.com", "Password123!");
        String token = loginAndExtractToken("reporter@example.com", "Password123!");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "USER",
                                  "reportedUserId": %d,
                                  "reason": "This user is asking for payment outside the platform."
                                }
                                """.formatted(reportedUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportType").value("USER"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reportedUserId").value(reportedUser.getId()));
    }

    @Test
    void userCannotReportThemselfOrDuplicateOpenTarget() throws Exception {
        User reporter = createUser("reporter@example.com", "Password123!");
        User reportedUser = createUser("reported@example.com", "Password123!");
        String token = loginAndExtractToken("reporter@example.com", "Password123!");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "USER",
                                  "reportedUserId": %d,
                                  "reason": "Trying to report myself should fail."
                                }
                                """.formatted(reporter.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot report yourself"));

        String reportBody = """
                {
                  "reportType": "USER",
                  "reportedUserId": %d,
                  "reason": "This user is asking for payment outside the platform."
                }
                """.formatted(reportedUser.getId());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You already have an open report for this user"));
    }

    @Test
    void invalidReportTargetCombinationFails() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        User reportedUser = createUser("reported@example.com", "Password123!");
        createUser("reporter@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("reporter@example.com", "Password123!");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "LISTING",
                                  "reportedUserId": %d,
                                  "reportedListingId": %d,
                                  "reason": "This invalid target combination should fail."
                                }
                                """.formatted(reportedUser.getId(), listing.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("LISTING reports require reportedListingId only"));
    }

    @Test
    void getMyReportsReturnsOnlyAuthenticatedUsersReports() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("reporter@example.com", "Password123!");
        User otherReporter = createUser("other@example.com", "Password123!");
        Listing listing = createListing(owner);
        String reporterToken = loginAndExtractToken("reporter@example.com", "Password123!");
        String otherToken = loginAndExtractToken("other@example.com", "Password123!");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "LISTING",
                                  "reportedListingId": %d,
                                  "reason": "This listing looks suspicious and uses copied photos."
                                }
                                """.formatted(listing.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "USER",
                                  "reportedUserId": %d,
                                  "reason": "This user is asking for payment outside the platform."
                                }
                                """.formatted(owner.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reports/me")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reportedListingId").value(listing.getId()))
                .andExpect(jsonPath("$[0].reporter").doesNotExist());
        org.assertj.core.api.Assertions.assertThat(otherReporter.getId()).isNotNull();
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

    private Listing createListing(User owner) {
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
        listing.setStatus(ListingStatus.AVAILABLE);
        listing.setExactAddress("123 Private Street");
        return listingRepository.save(listing);
    }
}
