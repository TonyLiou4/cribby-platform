package com.santosh.roommate_marketplace_api.report.controller;

import com.santosh.roommate_marketplace_api.report.dto.AdminReportResponse;
import com.santosh.roommate_marketplace_api.report.dto.AdminReportStatusUpdateRequest;
import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import com.santosh.roommate_marketplace_api.report.service.AdminReportModerationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin Reports", description = "Admin-only report moderation endpoints")
public class AdminReportModerationController {

    private final AdminReportModerationService adminReportModerationService;

    public AdminReportModerationController(AdminReportModerationService adminReportModerationService) {
        this.adminReportModerationService = adminReportModerationService;
    }

    @GetMapping
    public List<AdminReportResponse> getReports(@RequestParam(required = false) ReportStatus status) {
        return adminReportModerationService.getReports(status);
    }

    @PatchMapping("/{id}/status")
    public AdminReportResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminReportStatusUpdateRequest request
    ) {
        return adminReportModerationService.updateStatus(id, request.status());
    }
}
