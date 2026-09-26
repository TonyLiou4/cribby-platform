package com.santosh.roommate_marketplace_api.verification.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.common.exception.ResourceNotFoundException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.verification.dto.AdminVerificationRequestResponse;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationType;
import com.santosh.roommate_marketplace_api.verification.repository.VerificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVerificationReviewServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @InjectMocks
    private AdminVerificationReviewService adminVerificationReviewService;

    @Test
    void listRequestsCanFilterByStatus() {
        VerificationRequest request = verificationRequest(VerificationType.STUDENT, VerificationStatus.PENDING);
        when(verificationRequestRepository.findByStatusOrderByCreatedAtDesc(VerificationStatus.PENDING))
                .thenReturn(List.of(request));

        List<AdminVerificationRequestResponse> response =
                adminVerificationReviewService.getVerificationRequests(VerificationStatus.PENDING);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void approveStudentRequestSetsStudentVerified() {
        VerificationRequest request = verificationRequest(VerificationType.STUDENT, VerificationStatus.PENDING);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        AdminVerificationRequestResponse response = adminVerificationReviewService.approve(1L);

        assertThat(response.status()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(response.reviewedAt()).isNotNull();
        assertThat(request.getUser().isStudentVerified()).isTrue();
        assertThat(request.getUser().isAlumniVerified()).isFalse();
    }

    @Test
    void approveAlumniRequestSetsAlumniVerified() {
        VerificationRequest request = verificationRequest(VerificationType.ALUMNI, VerificationStatus.PENDING);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        AdminVerificationRequestResponse response = adminVerificationReviewService.approve(1L);

        assertThat(response.status()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(request.getUser().isAlumniVerified()).isTrue();
    }

    @Test
    void rejectRequestStoresReason() {
        VerificationRequest request = verificationRequest(VerificationType.STUDENT, VerificationStatus.PENDING);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        AdminVerificationRequestResponse response = adminVerificationReviewService.reject(1L, "Document is unreadable");

        assertThat(response.status()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Document is unreadable");
        assertThat(response.reviewedAt()).isNotNull();
    }

    @Test
    void alreadyReviewedRequestCannotBeReviewedAgain() {
        VerificationRequest request = verificationRequest(VerificationType.STUDENT, VerificationStatus.APPROVED);
        when(verificationRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> adminVerificationReviewService.reject(1L, "No longer pending"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been reviewed");
    }

    @Test
    void missingRequestThrowsNotFound() {
        when(verificationRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminVerificationReviewService.approve(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private VerificationRequest verificationRequest(VerificationType type, VerificationStatus status) {
        User user = new User();
        user.setEmail("student@example.com");
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, 10L);

        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setVerificationType(type);
        request.setStatus(status);
        request.setProofDocumentUrl("https://example.com/proof.jpg");
        setId(request, 1L);
        return request;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = com.santosh.roommate_marketplace_api.common.entity.BaseEntity.class
                    .getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
