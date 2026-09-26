package com.santosh.roommate_marketplace_api.auth.email.controller;

import com.santosh.roommate_marketplace_api.auth.email.dto.EmailVerificationResponse;
import com.santosh.roommate_marketplace_api.auth.email.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Email Verification", description = "Email verification token endpoints")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/send-verification-email")
    @Operation(summary = "Send a verification email to the authenticated user")
    public EmailVerificationResponse sendVerificationEmail(Authentication authentication) {
        return emailVerificationService.sendVerificationEmail(authentication.getName());
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify an email address using a one-time token")
    public EmailVerificationResponse verifyEmail(@RequestParam @NotBlank String token) {
        return emailVerificationService.verifyEmail(token);
    }

    @PostMapping("/resend-verification-email")
    @Operation(summary = "Resend a verification email to the authenticated user")
    public EmailVerificationResponse resendVerificationEmail(Authentication authentication) {
        return emailVerificationService.resendVerificationEmail(authentication.getName());
    }
}
