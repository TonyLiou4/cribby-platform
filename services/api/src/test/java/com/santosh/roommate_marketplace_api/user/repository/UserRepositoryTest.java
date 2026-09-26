package com.santosh.roommate_marketplace_api.user.repository;

import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Test
    void userCanBeSavedAndRetrievedByEmail() {
        userRepository.save(testUser("saved@example.com"));

        assertThat(userRepository.findByEmailIgnoreCase("SAVED@example.com"))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("saved@example.com");
    }

    @Test
    void userEmailUniquenessIsEnforced() {
        userRepository.saveAndFlush(testUser("unique@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(testUser("unique@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void basicListingRelationshipDoesNotBreakJpaContext() {
        User owner = userRepository.saveAndFlush(testUser("owner@example.com"));

        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room near campus");
        listing.setDescription("Clean private room for a student.");
        listing.setCity("Boston");
        listing.setNeighborhood("Fenway");
        listing.setUniversityName("Northeastern University");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setUtilitiesIncluded(true);
        listing.setAvailableFrom(LocalDate.now().plusDays(30));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(ListingStatus.AVAILABLE);

        Listing savedListing = listingRepository.saveAndFlush(listing);

        assertThat(savedListing.getId()).isNotNull();
        assertThat(savedListing.getOwner().getEmail()).isEqualTo("owner@example.com");
    }

    private User testUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setUniversityName("Northeastern University");
        user.setUserType(UserType.STUDENT);

        UserProfile profile = new UserProfile();
        profile.setDisplayName("Test Student");
        user.setProfile(profile);
        return user;
    }
}
