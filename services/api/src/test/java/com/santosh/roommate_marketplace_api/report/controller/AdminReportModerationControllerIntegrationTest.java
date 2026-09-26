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
import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import com.santosh.roommate_marketplace_api.user.entity.Role;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import com.santosh.roommate_marketplace_api.verification.repository.VerificationRequestRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class AdminReportModerationControllerIntegrationTest {

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
    private VerificationRequestRepository verificationRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        verificationRequestRepository.deleteAll();
        reportRepository.deleteAll();
        favoriteRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminReportEndpointsRequireAuthenticationAndAdminRole() throws Exception {
        createUser("user@example.com", "Password123!", Role.USER);
        String userToken = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListReportsFilteredByStatusWithSafeSummaryData() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User reporter = createUser("reporter@example.com", "Password123!", Role.USER);
        User owner = createUser("owner@example.com", "Password123!", Role.USER);
        Listing listing = createListing(owner);
        createReport(reporter, listing, ReportStatus.OPEN);
        createReport(reporter, listing, ReportStatus.RESOLVED);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].reporter.email").value("reporter@example.com"))
                .andExpect(jsonPath("$[0].reporter.password").doesNotExist())
                .andExpect(jsonPath("$[0].reportedListing.title").value("Private room"))
                .andExpect(jsonPath("$[0].reportedListing.exactAddress").doesNotExist());
    }

    @Test
    void adminCanUpdateReportStatus() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User reporter = createUser("reporter@example.com", "Password123!", Role.USER);
        User owner = createUser("owner@example.com", "Password123!", Role.USER);
        Listing listing = createListing(owner);
        Report report = createReport(reporter, listing, ReportStatus.OPEN);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/reports/{id}/status", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REVIEWED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));

        org.assertj.core.api.Assertions.assertThat(
                reportRepository.findById(report.getId()).orElseThrow().getStatus()
        ).isEqualTo(ReportStatus.REVIEWED);
    }

    @Test
    void adminCanResolveOrDismissReport() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User reporter = createUser("reporter@example.com", "Password123!", Role.USER);
        User owner = createUser("owner@example.com", "Password123!", Role.USER);
        Listing listing = createListing(owner);
        Report resolvedReport = createReport(reporter, listing, ReportStatus.OPEN);
        Report dismissedReport = createReport(reporter, listing, ReportStatus.REVIEWED);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/reports/{id}/status", resolvedReport.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(patch("/api/admin/reports/{id}/status", dismissedReport.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISMISSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    void reportStatusCannotBeChangedBackToOpenAndMissingReportReturnsNotFound() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User reporter = createUser("reporter@example.com", "Password123!", Role.USER);
        User owner = createUser("owner@example.com", "Password123!", Role.USER);
        Listing listing = createListing(owner);
        Report report = createReport(reporter, listing, ReportStatus.REVIEWED);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/reports/{id}/status", report.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OPEN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Report status cannot be changed back to OPEN"));

        mockMvc.perform(patch("/api/admin/reports/999999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REVIEWED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Report not found"));
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

    private User createUser(String email, String password, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserType(UserType.STUDENT);
        user.setRole(role);

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

    private Report createReport(User reporter, Listing listing, ReportStatus status) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedListing(listing);
        report.setReportType(ReportType.LISTING);
        report.setReason("This listing looks suspicious.");
        report.setStatus(status);
        return reportRepository.save(report);
    }
}
