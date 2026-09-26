package com.santosh.roommate_marketplace_api.report.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.report.dto.AdminReportResponse;
import com.santosh.roommate_marketplace_api.report.entity.Report;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminReportModerationService {

    private final ReportRepository reportRepository;

    public AdminReportModerationService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminReportResponse> getReports(ReportStatus status) {
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);

        return reports.stream()
                .map(AdminReportResponse::from)
                .toList();
    }

    @Transactional
    public AdminReportResponse updateStatus(Long id, ReportStatus status) {
        if (status == ReportStatus.OPEN) {
            throw new BadRequestException("Report status cannot be changed back to OPEN");
        }

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(status);

        return AdminReportResponse.from(report);
    }
}
