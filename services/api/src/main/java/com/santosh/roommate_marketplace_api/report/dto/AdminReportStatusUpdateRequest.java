package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportStatusUpdateRequest(
        @NotNull
        ReportStatus status
) {
}
