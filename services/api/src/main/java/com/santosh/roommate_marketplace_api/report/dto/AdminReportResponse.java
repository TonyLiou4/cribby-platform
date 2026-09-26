package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;

import java.time.LocalDateTime;

public record AdminReportResponse(
        Long id,
        ReportType reportType,
        ReportStatus status,
        String reason,
        AdminReportUserSummary reporter,
        AdminReportUserSummary reportedUser,
        AdminReportListingSummary reportedListing,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getReportType(),
                report.getStatus(),
                report.getReason(),
                AdminReportUserSummary.from(report.getReporter()),
                AdminReportUserSummary.from(report.getReportedUser()),
                AdminReportListingSummary.from(report.getReportedListing()),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
