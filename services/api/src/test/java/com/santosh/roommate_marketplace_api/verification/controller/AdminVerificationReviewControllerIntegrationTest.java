package com.santosh.roommate_marketplace_api.verification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.favorite.repository.FavoriteRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingImageRepository;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import com.santosh.roommate_marketplace_api.user.entity.Role;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class AdminVerificationReviewControllerIntegrationTest {

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
    void adminEndpointsRequireAuthenticationAndAdminRole() throws Exception {
        createUser("user@example.com", "Password123!", Role.USER);
        String userToken = loginAndExtractToken("user@example.com", "Password123!");

        mockMvc.perform(get("/api/admin/verification-requests"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/verification-requests")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListVerificationRequestsFilteredByStatus() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User student = createUser("student@example.com", "Password123!", Role.USER);
        createVerificationRequest(student, VerificationType.STUDENT, VerificationStatus.PENDING);
        createVerificationRequest(student, VerificationType.ALUMNI, VerificationStatus.REJECTED);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(get("/api/admin/verification-requests")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].userEmail").value("student@example.com"));
    }

    @Test
    void adminCanApproveStudentVerificationAndSetUserFlag() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User student = createUser("student@example.com", "Password123!", Role.USER);
        VerificationRequest request = createVerificationRequest(student, VerificationType.STUDENT, VerificationStatus.PENDING);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/verification-requests/{id}/approve", request.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        User refreshedStudent = userRepository.findById(student.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(refreshedStudent.isStudentVerified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(refreshedStudent.isAlumniVerified()).isFalse();
    }

    @Test
    void adminCanApproveAlumniVerificationAndSetUserFlag() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User alumni = createUser("alumni@example.com", "Password123!", Role.USER);
        VerificationRequest request = createVerificationRequest(alumni, VerificationType.ALUMNI, VerificationStatus.PENDING);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/verification-requests/{id}/approve", request.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        User refreshedAlumni = userRepository.findById(alumni.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(refreshedAlumni.isAlumniVerified()).isTrue();
    }

    @Test
    void adminCanRejectVerificationWithReason() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User student = createUser("student@example.com", "Password123!", Role.USER);
        VerificationRequest request = createVerificationRequest(student, VerificationType.STUDENT, VerificationStatus.PENDING);
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/verification-requests/{id}/reject", request.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "Document is unreadable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Document is unreadable"))
                .andExpect(jsonPath("$.reviewedAt").exists());
    }

    @Test
    void rejectionReasonIsRequiredAndReviewedRequestsCannotBeReviewedAgain() throws Exception {
        User admin = createUser("admin@example.com", "Password123!", Role.ADMIN);
        User student = createUser("student@example.com", "Password123!", Role.USER);
        VerificationRequest approvedRequest = createVerificationRequest(
                student,
                VerificationType.STUDENT,
                VerificationStatus.APPROVED
        );
        VerificationRequest pendingRequest = createVerificationRequest(
                student,
                VerificationType.ALUMNI,
                VerificationStatus.PENDING
        );
        String adminToken = loginAndExtractToken(admin.getEmail(), "Password123!");

        mockMvc.perform(patch("/api/admin/verification-requests/{id}/reject", pendingRequest.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/verification-requests/{id}/approve", approvedRequest.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification request has already been reviewed"));
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

    private VerificationRequest createVerificationRequest(
            User user,
            VerificationType type,
            VerificationStatus status
    ) {
        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setVerificationType(type);
        request.setStatus(status);
        request.setProofDocumentUrl("https://example.com/proof.jpg");
        return verificationRequestRepository.save(request);
    }
}
