package com.santosh.roommate_marketplace_api.auth.email.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.auth.email.repository.EmailVerificationTokenRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class EmailVerificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void sendVerificationEmailRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/send-verification-email"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendVerificationEmailRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification-email"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyEmailIsPublicButRequiresValidToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired email verification token"));
    }

    @Test
    void sendVerificationEmailForAuthenticatedUnverifiedUserReturnsSuccessAndStoresHashOnly() throws Exception {
        createUser("send.email@example.com", "Password123!", false);
        String accessToken = loginAndExtractAccessToken("send.email@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If your account needs verification, a verification email will be sent."));

        assertThat(tokenRepository.findAll()).hasSize(1);
        String rawToken = extractTokenFromSentEmail();
        assertThat(tokenRepository.findAll().getFirst().getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void sendVerificationEmailForAlreadyVerifiedUserReturnsSafeSuccessWithoutSending() throws Exception {
        createUser("verified.email@example.com", "Password123!", true);
        String accessToken = loginAndExtractAccessToken("verified.email@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If your account needs verification, a verification email will be sent."));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(tokenRepository.findAll()).isEmpty();
    }

    @Test
    void verifyValidTokenMarksEmailVerifiedAndTokenUsed() throws Exception {
        User user = createUser("verify.email@example.com", "Password123!", false);
        String accessToken = loginAndExtractAccessToken("verify.email@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String rawToken = extractTokenFromSentEmail();

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully."));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.isEmailVerified()).isTrue();
        assertThat(tokenRepository.findAll().getFirst().isUsed()).isTrue();
    }

    @Test
    void expiredAndAlreadyUsedTokensFail() throws Exception {
        createUser("expired.email@example.com", "Password123!", false);
        String accessToken = loginAndExtractAccessToken("expired.email@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String rawToken = extractTokenFromSentEmail();
        var savedToken = tokenRepository.findAll().getFirst();
        savedToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(savedToken);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", rawToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired email verification token"));

        savedToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        savedToken.markUsed();
        tokenRepository.save(savedToken);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", rawToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired email verification token"));
    }

    @Test
    void resendVerificationEmailCreatesNewTokenAndInvalidatesPreviousOne() throws Exception {
        createUser("resend.email@example.com", "Password123!", false);
        String accessToken = loginAndExtractAccessToken("resend.email@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        String firstRawToken = extractTokenFromSentEmail();

        mockMvc.perform(post("/api/auth/resend-verification-email")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If your account needs verification, a verification email will be sent."));
        String secondRawToken = extractTokenFromSentEmail();

        assertThat(secondRawToken).isNotEqualTo(firstRawToken);
        assertThat(tokenRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", firstRawToken))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndExtractAccessToken(String email, String password) throws Exception {
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
        return json.get("accessToken").asText();
    }

    private String extractTokenFromSentEmail() {
        org.mockito.ArgumentCaptor<SimpleMailMessage> captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        String body = captor.getAllValues().getLast().getText();
        assertThat(body).contains("/api/auth/verify-email?token=");
        return body.substring(body.indexOf("token=") + "token=".length()).lines().findFirst().orElseThrow().trim();
    }

    private User createUser(String email, String password, boolean emailVerified) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUniversityName("Northeastern University");
        user.setUserType(UserType.STUDENT);
        user.setEmailVerified(emailVerified);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test Student");
        profile.setUniversity("Northeastern University");
        user.setProfile(profile);

        return userRepository.save(user);
    }
}
