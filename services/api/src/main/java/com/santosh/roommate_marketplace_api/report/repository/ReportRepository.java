package com.santosh.roommate_marketplace_api.report.repository;

import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndReportedUserIdAndStatus(Long reporterId, Long reportedUserId, ReportStatus status);

    boolean existsByReporterIdAndReportedListingIdAndStatus(Long reporterId, Long reportedListingId, ReportStatus status);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<Report> findAllByOrderByCreatedAtDesc();
}
