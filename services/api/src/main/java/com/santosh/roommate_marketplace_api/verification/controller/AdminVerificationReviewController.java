package com.santosh.roommate_marketplace_api.verification.controller;

import com.santosh.roommate_marketplace_api.verification.dto.AdminVerificationRequestResponse;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRejectRequest;
import com.santosh.roommate_marketplace_api.verification.entity.VerificationStatus;
import com.santosh.roommate_marketplace_api.verification.service.AdminVerificationReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/verification-requests")
@Tag(name = "Admin Verification", description = "Admin-only verification review endpoints")
public class AdminVerificationReviewController {

    private final AdminVerificationReviewService adminVerificationReviewService;

    public AdminVerificationReviewController(AdminVerificationReviewService adminVerificationReviewService) {
        this.adminVerificationReviewService = adminVerificationReviewService;
    }

    @GetMapping
    public List<AdminVerificationRequestResponse> getVerificationRequests(
            @RequestParam(required = false) VerificationStatus status
    ) {
        return adminVerificationReviewService.getVerificationRequests(status);
    }

    @PatchMapping("/{id}/approve")
    public AdminVerificationRequestResponse approve(@PathVariable Long id) {
        return adminVerificationReviewService.approve(id);
    }

    @PatchMapping("/{id}/reject")
    public AdminVerificationRequestResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody VerificationRejectRequest request
    ) {
        return adminVerificationReviewService.reject(id, request.rejectionReason());
    }
}
