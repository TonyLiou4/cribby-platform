package com.santosh.roommate_marketplace_api.verification.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestCreateRequest;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestResponse;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
import com.santosh.roommate_marketplace_api.verification.repository.VerificationRequestRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VerificationRequestService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final UserRepository userRepository;

    public VerificationRequestService(
            VerificationRequestRepository verificationRequestRepository,
            UserRepository userRepository
    ) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VerificationRequestResponse createRequest(
            String userEmail,
            VerificationRequestCreateRequest request
    ) {
        User user = getUserByEmail(userEmail);

        if (request.verificationType() != VerificationType.STUDENT
                && request.verificationType() != VerificationType.ALUMNI) {
            throw new BadRequestException("Only STUDENT and ALUMNI verification requests are supported right now");
        }

        if (verificationRequestRepository.existsByUserIdAndVerificationTypeAndStatus(
                user.getId(),
                request.verificationType(),
                VerificationStatus.PENDING
        )) {
            throw new BadRequestException("You already have a pending request for this verification type");
        }

        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUser(user);
        verificationRequest.setVerificationType(request.verificationType());
        verificationRequest.setStatus(VerificationStatus.PENDING);
        verificationRequest.setProofDocumentUrl(request.proofDocumentUrl().trim());

        return VerificationRequestResponse.from(verificationRequestRepository.save(verificationRequest));
    }

    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> getMyRequests(String userEmail) {
        User user = getUserByEmail(userEmail);
        return verificationRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(VerificationRequestResponse::from)
                .toList();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
