package com.santosh.roommate_marketplace_api.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santosh.roommate_marketplace_api.TestDataCleanup;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "rate-limit.enabled=true",
        "rate-limit.policies.login.capacity=1",
        "rate-limit.policies.login.refill-period=PT1H",
        "rate-limit.policies.verification-email.capacity=1",
        "rate-limit.policies.verification-email.refill-period=PT1H"
})
@TestDataCleanup
class RateLimitFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void loginAllowsRequestThenBlocksByIp() throws Exception {
        createUser("limited.login@example.com", "Password123!", true);

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "limited.login@example.com",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "limited.login@example.com",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, notNullValue()))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void sendVerificationEmailAllowsRequestThenBlocksByAuthenticatedUser() throws Exception {
        createUser("limited.email@example.com", "Password123!", false);
        String accessToken = loginAndExtractAccessToken("limited.email@example.com", "Password123!", "203.0.113.20");

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("X-Forwarded-For", "203.0.113.21"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/send-verification-email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("X-Forwarded-For", "203.0.113.22"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/send-verification-email"));
    }

    private String loginAndExtractAccessToken(String email, String password, String ipAddress) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", ipAddress)
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
