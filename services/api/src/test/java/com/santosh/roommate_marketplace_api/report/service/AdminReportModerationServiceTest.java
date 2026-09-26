package com.santosh.roommate_marketplace_api.report.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.entity.GenderPreference;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.entity.ListingStatus;
import com.santosh.roommate_marketplace_api.listing.entity.RoomType;
import com.santosh.roommate_marketplace_api.report.dto.AdminReportResponse;
import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportModerationServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminReportModerationService adminReportModerationService;

    @Test
    void listReportsCanFilterByStatus() {
        Report report = report(ReportStatus.OPEN);
        when(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.OPEN)).thenReturn(List.of(report));

        List<AdminReportResponse> response = adminReportModerationService.getReports(ReportStatus.OPEN);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(ReportStatus.OPEN);
        assertThat(response.get(0).reporter().email()).isEqualTo("reporter@example.com");
        assertThat(response.get(0).reportedListing().title()).isEqualTo("Private room");
    }

    @Test
    void adminCanSetReviewedResolvedOrDismissed() {
        Report report = report(ReportStatus.OPEN);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        AdminReportResponse response = adminReportModerationService.updateStatus(1L, ReportStatus.RESOLVED);

        assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void adminCannotChangeStatusBackToOpen() {
        assertThatThrownBy(() -> adminReportModerationService.updateStatus(1L, ReportStatus.OPEN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OPEN");
    }

    @Test
    void missingReportThrowsNotFound() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminReportModerationService.updateStatus(99L, ReportStatus.REVIEWED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Report report(ReportStatus status) {
        Report report = new Report();
        report.setReporter(user("reporter@example.com"));
        report.setReportedListing(listing(user("owner@example.com")));
        report.setReportType(ReportType.LISTING);
        report.setReason("This listing looks suspicious.");
        report.setStatus(status);
        setId(report, 1L);
        return report;
    }

    private User user(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, email.equals("reporter@example.com") ? 10L : 20L);
        return user;
    }

    private Listing listing(User owner) {
        Listing listing = new Listing();
        listing.setOwner(owner);
        listing.setTitle("Private room");
        listing.setDescription("Clean room.");
        listing.setCity("Boston");
        listing.setMonthlyRent(new BigDecimal("1200.00"));
        listing.setRoomType(RoomType.PRIVATE_ROOM);
        listing.setGenderPreference(GenderPreference.ANY);
        listing.setStatus(ListingStatus.AVAILABLE);
        setId(listing, 30L);
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
