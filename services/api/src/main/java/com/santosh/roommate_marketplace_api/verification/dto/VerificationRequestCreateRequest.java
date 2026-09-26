package com.santosh.roommate_marketplace_api.verification.dto;

import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerificationRequestCreateRequest(
        @NotNull
        VerificationType verificationType,

        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://")
        String proofDocumentUrl
) {
}
