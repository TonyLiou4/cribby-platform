package com.santosh.roommate_marketplace_api.verification.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.verification.dto.AdminVerificationRequestResponse;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
import com.santosh.roommate_marketplace_api.verification.repository.VerificationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminVerificationReviewService {

    private final VerificationRequestRepository verificationRequestRepository;

    public AdminVerificationReviewService(VerificationRequestRepository verificationRequestRepository) {
        this.verificationRequestRepository = verificationRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminVerificationRequestResponse> getVerificationRequests(VerificationStatus status) {
        List<VerificationRequest> requests = status == null
                ? verificationRequestRepository.findAllByOrderByCreatedAtDesc()
                : verificationRequestRepository.findByStatusOrderByCreatedAtDesc(status);

        return requests.stream()
                .map(AdminVerificationRequestResponse::from)
                .toList();
    }

    @Transactional
    public AdminVerificationRequestResponse approve(Long id) {
        VerificationRequest request = getPendingRequest(id);
        User user = request.getUser();

        if (request.getVerificationType() == VerificationType.STUDENT) {
            user.setStudentVerified(true);
        } else if (request.getVerificationType() == VerificationType.ALUMNI) {
            user.setAlumniVerified(true);
        } else {
            throw new BadRequestException("Only STUDENT and ALUMNI verification requests can be approved right now");
        }

        request.setStatus(VerificationStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(null);

        return AdminVerificationRequestResponse.from(request);
    }

    @Transactional
    public AdminVerificationRequestResponse reject(Long id, String rejectionReason) {
        VerificationRequest request = getPendingRequest(id);
        request.setStatus(VerificationStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setRejectionReason(rejectionReason.trim());

        return AdminVerificationRequestResponse.from(request);
    }

    private VerificationRequest getPendingRequest(Long id) {
        VerificationRequest request = verificationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        if (request.getStatus() != VerificationStatus.PENDING) {
            throw new BadRequestException("Verification request has already been reviewed");
        }

        return request;
    }
}
