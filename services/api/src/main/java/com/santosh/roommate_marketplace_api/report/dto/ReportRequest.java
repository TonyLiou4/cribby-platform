package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.report.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull
        ReportType reportType,

        Long reportedUserId,

        Long reportedListingId,

        @NotBlank
        @Size(min = 10, max = 1000)
        String reason
) {
}
