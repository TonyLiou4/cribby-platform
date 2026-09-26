package com.santosh.roommate_marketplace_api.verification.dto;

import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;

import java.time.LocalDateTime;

public record VerificationRequestResponse(
        Long id,
        VerificationType verificationType,
        VerificationStatus status,
        String proofDocumentUrl,
        String rejectionReason,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static VerificationRequestResponse from(VerificationRequest request) {
        return new VerificationRequestResponse(
                request.getId(),
                request.getVerificationType(),
                request.getStatus(),
                request.getProofDocumentUrl(),
                request.getRejectionReason(),
                request.getReviewedAt(),
                request.getCreatedAt()
        );
    }
}
