package com.santosh.roommate_marketplace_api.verification.dto;

import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;

import java.time.LocalDateTime;

public record AdminVerificationRequestResponse(
        Long id,
        Long userId,
        String userEmail,
        VerificationType verificationType,
        VerificationStatus status,
        String proofDocumentUrl,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static AdminVerificationRequestResponse from(VerificationRequest request) {
        return new AdminVerificationRequestResponse(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getEmail(),
                request.getVerificationType(),
                request.getStatus(),
                request.getProofDocumentUrl(),
                request.getRejectionReason(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }
}
