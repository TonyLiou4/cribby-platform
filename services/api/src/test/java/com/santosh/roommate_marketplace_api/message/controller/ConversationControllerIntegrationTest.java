package com.santosh.roommate_marketplace_api.message.controller;

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
import com.santosh.roommate_marketplace_api.message.entity.Conversation;
import com.santosh.roommate_marketplace_api.message.repository.ConversationRepository;
import com.santosh.roommate_marketplace_api.message.repository.MessageRepository;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestDataCleanup
class ConversationControllerIntegrationTest {

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
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        verificationRequestRepository.deleteAll();
        reportRepository.deleteAll();
        favoriteRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void messagingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId": 1}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/conversations/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/conversations/1/messages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/conversations/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanStartConversationWithListingOwnerAndDuplicateReturnsExisting() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("sender@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("sender@example.com", "Password123!");

        String firstResponse = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId": %d}
                                """.formatted(listing.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId", notNullValue()))
                .andExpect(jsonPath("$.listing.id").value(listing.getId()))
                .andExpect(jsonPath("$.otherParticipant.id").value(owner.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long conversationId = objectMapper.readTree(firstResponse).get("conversationId").asLong();

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId": %d}
                                """.formatted(listing.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(conversationId));

        org.assertj.core.api.Assertions.assertThat(conversationRepository.findAll()).hasSize(1);
    }

    @Test
    void userCannotStartConversationForOwnListing() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        Listing listing = createListing(owner);
        String token = loginAndExtractToken("owner@example.com", "Password123!");

        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId": %d}
                                """.formatted(listing.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot start a conversation with yourself for your own listing"));
    }

    @Test
    void participantsCanSendAndViewMessagesOrderedAscending() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("sender@example.com", "Password123!");
        Listing listing = createListing(owner);
        String senderToken = loginAndExtractToken("sender@example.com", "Password123!");
        String ownerToken = loginAndExtractToken("owner@example.com", "Password123!");
        Long conversationId = startConversationAndGetId(senderToken, listing.getId());

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Hello, is this room still available?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello, is this room still available?"));

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Yes, it is available."}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("Hello, is this room still available?"))
                .andExpect(jsonPath("$[1].content").value("Yes, it is available."));
    }

    @Test
    void onlyParticipantsCanViewOrSendMessages() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("sender@example.com", "Password123!");
        createUser("other@example.com", "Password123!");
        Listing listing = createListing(owner);
        String senderToken = loginAndExtractToken("sender@example.com", "Password123!");
        String otherToken = loginAndExtractToken("other@example.com", "Password123!");
        Long conversationId = startConversationAndGetId(senderToken, listing.getId());

        mockMvc.perform(get("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "I should not be able to send this."}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void conversationListShowsOtherParticipantListingAndLastMessagePreview() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("sender@example.com", "Password123!");
        Listing listing = createListing(owner);
        String senderToken = loginAndExtractToken("sender@example.com", "Password123!");
        Long conversationId = startConversationAndGetId(senderToken, listing.getId());

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Hello, is this room still available?"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/conversations/me")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId").value(conversationId))
                .andExpect(jsonPath("$[0].listing.title").value("Private room"))
                .andExpect(jsonPath("$[0].otherParticipant.id").value(owner.getId()))
                .andExpect(jsonPath("$[0].lastMessagePreview").value("Hello, is this room still available?"))
                .andExpect(jsonPath("$[0].lastUpdatedAt").exists());
    }

    @Test
    void blankMessageContentFailsValidation() throws Exception {
        User owner = createUser("owner@example.com", "Password123!");
        createUser("sender@example.com", "Password123!");
        Listing listing = createListing(owner);
        String senderToken = loginAndExtractToken("sender@example.com", "Password123!");
        Long conversationId = startConversationAndGetId(senderToken, listing.getId());

        mockMvc.perform(post("/api/conversations/{conversationId}/messages", conversationId)
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Long startConversationAndGetId(String token, Long listingId) throws Exception {
        String response = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"listingId": %d}
                                """.formatted(listingId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("conversationId").asLong();
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
        user.setRole(Role.USER);

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
