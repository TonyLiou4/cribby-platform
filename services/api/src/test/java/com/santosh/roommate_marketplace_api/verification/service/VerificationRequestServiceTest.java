package com.santosh.roommate_marketplace_api.verification.service;

import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestCreateRequest;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationRequestServiceTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationRequestService verificationRequestService;

    @Test
    void userCanCreateStudentVerificationRequest() {
        User user = user(1L, "student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRequestRepository.existsByUserIdAndVerificationTypeAndStatus(
                1L,
                VerificationType.STUDENT,
                VerificationStatus.PENDING
        )).thenReturn(false);
        when(verificationRequestRepository.save(any(VerificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequestResponse response = verificationRequestService.createRequest(
                "student@example.com",
                new VerificationRequestCreateRequest(
                        VerificationType.STUDENT,
                        "https://example.com/student-id.jpg"
                )
        );

        assertThat(response.verificationType()).isEqualTo(VerificationType.STUDENT);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(response.proofDocumentUrl()).isEqualTo("https://example.com/student-id.jpg");
    }

    @Test
    void userCanCreateAlumniVerificationRequest() {
        User user = user(1L, "alumni@example.com");
        when(userRepository.findByEmailIgnoreCase("alumni@example.com")).thenReturn(Optional.of(user));
        when(verificationRequestRepository.existsByUserIdAndVerificationTypeAndStatus(
                1L,
                VerificationType.ALUMNI,
                VerificationStatus.PENDING
        )).thenReturn(false);
        when(verificationRequestRepository.save(any(VerificationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationRequestResponse response = verificationRequestService.createRequest(
                "alumni@example.com",
                new VerificationRequestCreateRequest(
                        VerificationType.ALUMNI,
                        "https://example.com/diploma.jpg"
                )
        );

        assertThat(response.verificationType()).isEqualTo(VerificationType.ALUMNI);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void phoneAndIdentityAreRejectedForNow() {
        User user = user(1L, "student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> verificationRequestService.createRequest(
                "student@example.com",
                new VerificationRequestCreateRequest(
                        VerificationType.PHONE,
                        "https://example.com/phone.jpg"
                )
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("STUDENT and ALUMNI");
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        User user = user(1L, "student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRequestRepository.existsByUserIdAndVerificationTypeAndStatus(
                1L,
                VerificationType.STUDENT,
                VerificationStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() -> verificationRequestService.createRequest(
                "student@example.com",
                new VerificationRequestCreateRequest(
                        VerificationType.STUDENT,
                        "https://example.com/student-id.jpg"
                )
        )).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pending request");
    }

    @Test
    void getMyRequestsReturnsUserRequests() {
        User user = user(1L, "student@example.com");
        VerificationRequest request = new VerificationRequest();
        request.setUser(user);
        request.setVerificationType(VerificationType.STUDENT);
        request.setStatus(VerificationStatus.PENDING);
        request.setProofDocumentUrl("https://example.com/student-id.jpg");

        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRequestRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(request));

        List<VerificationRequestResponse> response = verificationRequestService.getMyRequests("student@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).verificationType()).isEqualTo(VerificationType.STUDENT);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPasswordHash("hashed-password");
        user.setUserType(UserType.STUDENT);
        setId(user, id);
        return user;
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
