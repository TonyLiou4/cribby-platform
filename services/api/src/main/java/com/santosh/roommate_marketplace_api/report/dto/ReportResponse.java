package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.entity.ReportType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        ReportType reportType,
        ReportStatus status,
        Long reportedUserId,
        Long reportedListingId,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReportType(),
                report.getStatus(),
                report.getReportedUser() == null ? null : report.getReportedUser().getId(),
                report.getReportedListing() == null ? null : report.getReportedListing().getId(),
                report.getReason(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
