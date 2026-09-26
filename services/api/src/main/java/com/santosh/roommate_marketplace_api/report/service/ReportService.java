package com.santosh.roommate_marketplace_api.report.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.listing.entity.Listing;
import com.santosh.roommate_marketplace_api.listing.repository.ListingRepository;
import com.santosh.roommate_marketplace_api.report.dto.ReportRequest;
import com.santosh.roommate_marketplace_api.report.dto.ReportResponse;
import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ListingRepository listingRepository
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional
    public ReportResponse submitReport(String reporterEmail, ReportRequest request) {
        User reporter = getUserByEmail(reporterEmail);

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportType(request.reportType());
        report.setStatus(ReportStatus.OPEN);
        report.setReason(request.reason().trim());

        if (request.reportType() == ReportType.USER) {
            applyUserReport(report, reporter, request);
        } else if (request.reportType() == ReportType.LISTING) {
            applyListingReport(report, reporter, request);
        } else {
            throw new BadRequestException("Only USER and LISTING reports are supported right now");
        }

        return ReportResponse.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports(String reporterEmail) {
        User reporter = getUserByEmail(reporterEmail);
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId())
                .stream()
                .map(ReportResponse::from)
                .toList();
    }

    private void applyUserReport(Report report, User reporter, ReportRequest request) {
        if (request.reportedUserId() == null || request.reportedListingId() != null) {
            throw new BadRequestException("USER reports require reportedUserId only");
        }
        if (reporter.getId().equals(request.reportedUserId())) {
            throw new BadRequestException("You cannot report yourself");
        }

        User reportedUser = userRepository.findById(request.reportedUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));

        if (reportRepository.existsByReporterIdAndReportedUserIdAndStatus(
                reporter.getId(),
                reportedUser.getId(),
                ReportStatus.OPEN
        )) {
            throw new BadRequestException("You already have an open report for this user");
        }

        report.setReportedUser(reportedUser);
    }

    private void applyListingReport(Report report, User reporter, ReportRequest request) {
        if (request.reportedListingId() == null || request.reportedUserId() != null) {
            throw new BadRequestException("LISTING reports require reportedListingId only");
        }

        Listing reportedListing = listingRepository.findById(request.reportedListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Reported listing not found"));

        if (reportRepository.existsByReporterIdAndReportedListingIdAndStatus(
                reporter.getId(),
                reportedListing.getId(),
                ReportStatus.OPEN
        )) {
            throw new BadRequestException("You already have an open report for this listing");
        }

        report.setReportedListing(reportedListing);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
