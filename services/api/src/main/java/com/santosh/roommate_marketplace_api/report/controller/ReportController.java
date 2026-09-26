package com.santosh.roommate_marketplace_api.report.controller;

import com.santosh.roommate_marketplace_api.report.dto.ReportRequest;
import com.santosh.roommate_marketplace_api.report.dto.ReportResponse;
import com.santosh.roommate_marketplace_api.report.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "User-facing safety report endpoints")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse submitReport(
            Authentication authentication,
            @Valid @RequestBody ReportRequest request
    ) {
        return reportService.submitReport(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<ReportResponse> getMyReports(Authentication authentication) {
        return reportService.getMyReports(authentication.getName());
    }
}
