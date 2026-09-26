package com.santosh.roommate_marketplace_api.verification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingImageRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class VerificationRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationRequestRepository verificationRequestRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private ListingRepository listingRepository;

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
    void verificationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "STUDENT",
                                  "proofDocumentUrl": "https://example.com/student-id.jpg"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/verification-requests/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateStudentVerificationRequest() throws Exception {
        createUser("student@example.com", "Password123!");
        String token = loginAndExtractToken("student@example.com", "Password123!");

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "STUDENT",
                                  "proofDocumentUrl": "https://example.com/student-id.jpg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationType").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.proofDocumentUrl").value("https://example.com/student-id.jpg"));
    }

    @Test
    void authenticatedUserCanCreateAlumniVerificationRequest() throws Exception {
        createUser("alumni@example.com", "Password123!");
        String token = loginAndExtractToken("alumni@example.com", "Password123!");

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "ALUMNI",
                                  "proofDocumentUrl": "https://example.com/diploma.jpg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verificationType").value("ALUMNI"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void phoneIdentityAndInvalidUrlsAreRejected() throws Exception {
        createUser("student@example.com", "Password123!");
        String token = loginAndExtractToken("student@example.com", "Password123!");

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "PHONE",
                                  "proofDocumentUrl": "https://example.com/phone.jpg"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only STUDENT and ALUMNI verification requests are supported right now"));

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "STUDENT",
                                  "proofDocumentUrl": "not-a-url"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullVerificationTypeReturnsValidationError() throws Exception {
        createUser("student@example.com", "Password123!");
        String token = loginAndExtractToken("student@example.com", "Password123!");

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": null,
                                  "proofDocumentUrl": "https://example.com/doc.jpg"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("verificationType")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must not be null")));
    }

    @Test
    void duplicatePendingVerificationTypeIsRejected() throws Exception {
        createUser("student@example.com", "Password123!");
        String token = loginAndExtractToken("student@example.com", "Password123!");
        String body = """
                {
                  "verificationType": "STUDENT",
                  "proofDocumentUrl": "https://example.com/student-id.jpg"
                }
                """;

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You already have a pending request for this verification type"));
    }

    @Test
    void getMyRequestsReturnsOnlyAuthenticatedUsersRequests() throws Exception {
        createUser("student@example.com", "Password123!");
        createUser("other@example.com", "Password123!");
        String studentToken = loginAndExtractToken("student@example.com", "Password123!");
        String otherToken = loginAndExtractToken("other@example.com", "Password123!");

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "STUDENT",
                                  "proofDocumentUrl": "https://example.com/student-id.jpg"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/verification-requests")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationType": "ALUMNI",
                                  "proofDocumentUrl": "https://example.com/diploma.jpg"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/verification-requests/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].verificationType").value("STUDENT"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
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
}
