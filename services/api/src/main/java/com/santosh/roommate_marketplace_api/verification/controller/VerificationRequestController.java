package com.santosh.roommate_marketplace_api.verification.controller;

import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestCreateRequest;
import com.santosh.roommate_marketplace_api.verification.dto.VerificationRequestResponse;
import com.santosh.roommate_marketplace_api.verification.service.VerificationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/verification-requests")
@Tag(name = "Verification Requests", description = "User-facing verification request endpoints")
public class VerificationRequestController {

    private final VerificationRequestService verificationRequestService;

    public VerificationRequestController(VerificationRequestService verificationRequestService) {
        this.verificationRequestService = verificationRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit a student or alumni verification request",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = VerificationRequestCreateRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Student verification",
                                            value = """
                                                    {
                                                      "verificationType": "STUDENT",
                                                      "proofDocumentUrl": "https://example.com/doc.jpg"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Alumni verification",
                                            value = """
                                                    {
                                                      "verificationType": "ALUMNI",
                                                      "proofDocumentUrl": "https://example.com/doc.jpg"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public VerificationRequestResponse createRequest(
            Authentication authentication,
            @Valid @org.springframework.web.bind.annotation.RequestBody VerificationRequestCreateRequest request
    ) {
        return verificationRequestService.createRequest(authentication.getName(), request);
    }

    @GetMapping("/me")
    public List<VerificationRequestResponse> getMyRequests(Authentication authentication) {
        return verificationRequestService.getMyRequests(authentication.getName());
    }
}
