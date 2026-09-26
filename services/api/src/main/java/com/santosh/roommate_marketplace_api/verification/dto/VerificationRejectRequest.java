package com.santosh.roommate_marketplace_api.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationRejectRequest(
        @NotBlank
        @Size(min = 5, max = 1000)
        String rejectionReason
) {
}
