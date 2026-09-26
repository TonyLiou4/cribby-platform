package com.santosh.roommate_marketplace_api.verification.repository;

import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    boolean existsByUserIdAndVerificationTypeAndStatus(
            Long userId,
            VerificationType verificationType,
            VerificationStatus status
    );

    List<VerificationRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<VerificationRequest> findByStatusOrderByCreatedAtDesc(VerificationStatus status);

    List<VerificationRequest> findAllByOrderByCreatedAtDesc();
}
