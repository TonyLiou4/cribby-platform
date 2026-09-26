package com.santosh.roommate_marketplace_api.report.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.report.dto.ReportRequest;
import com.santosh.roommate_marketplace_api.report.dto.ReportResponse;
import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void userCanReportListing() {
        User reporter = user(1L, "reporter@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"));

        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(reportRepository.existsByReporterIdAndReportedListingIdAndStatus(1L, 10L, ReportStatus.OPEN))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.submitReport(
                "reporter@example.com",
                new ReportRequest(ReportType.LISTING, null, 10L, "This listing looks suspicious.")
        );

        assertThat(response.reportType()).isEqualTo(ReportType.LISTING);
        assertThat(response.status()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.reportedListingId()).isEqualTo(10L);
    }

    @Test
    void userCanReportAnotherUser() {
        User reporter = user(1L, "reporter@example.com");
        User reportedUser = user(2L, "reported@example.com");

        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reportedUser));
        when(reportRepository.existsByReporterIdAndReportedUserIdAndStatus(1L, 2L, ReportStatus.OPEN))
                .thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse response = reportService.submitReport(
                "reporter@example.com",
                new ReportRequest(ReportType.USER, 2L, null, "This user is asking for off-platform payment.")
        );

        assertThat(response.reportType()).isEqualTo(ReportType.USER);
        assertThat(response.status()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.reportedUserId()).isEqualTo(2L);
    }

    @Test
    void userCannotReportThemself() {
        User reporter = user(1L, "reporter@example.com");
        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));

        assertThatThrownBy(() -> reportService.submitReport(
                "reporter@example.com",
                new ReportRequest(ReportType.USER, 1L, null, "I should not be able to report myself.")
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void duplicateOpenListingReportFails() {
        User reporter = user(1L, "reporter@example.com");
        Listing listing = listing(10L, user(2L, "owner@example.com"));

        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(listingRepository.findById(10L)).thenReturn(Optional.of(listing));
        when(reportRepository.existsByReporterIdAndReportedListingIdAndStatus(1L, 10L, ReportStatus.OPEN))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.submitReport(
                "reporter@example.com",
                new ReportRequest(ReportType.LISTING, null, 10L, "This listing looks suspicious.")
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("open report");
    }

    @Test
    void invalidTargetCombinationFails() {
        User reporter = user(1L, "reporter@example.com");
        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));

        assertThatThrownBy(() -> reportService.submitReport(
                "reporter@example.com",
                new ReportRequest(ReportType.LISTING, 2L, 10L, "Invalid target combination.")
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reportedListingId only");
    }

    @Test
    void getMyReportsReturnsReporterReports() {
        User reporter = user(1L, "reporter@example.com");
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(user(2L, "reported@example.com"));
        report.setReportType(ReportType.USER);
        report.setStatus(ReportStatus.OPEN);
        report.setReason("This user is asking for off-platform payment.");

        when(userRepository.findByEmailIgnoreCase("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(reportRepository.findByReporterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(report));

        List<ReportResponse> response = reportService.getMyReports("reporter@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).reportType()).isEqualTo(ReportType.USER);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, id);
        return user;
    }

    private Listing listing(Long id, User owner) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean room.");
        listing.setCity("Boston");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(ListingStatus.AVAILABLE);
        setId(listing, id);
        return listing;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = com.santosh.roommate_marketplace_api.common.entity.BaseEntity.class
                    .getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
