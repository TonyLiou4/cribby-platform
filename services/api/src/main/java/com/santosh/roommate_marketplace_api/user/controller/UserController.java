package com.santosh.roommate_marketplace_api.user.controller;

import com.santosh.roommate_marketplace_api.user.dto.CurrentUserResponse;
import com.santosh.roommate_marketplace_api.user.dto.PublicUserProfileResponse;
import com.santosh.roommate_marketplace_api.user.dto.UpdateProfileRequest;
import com.santosh.roommate_marketplace_api.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Current user and public profile endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return userService.getCurrentUserByEmail(authentication.getName());
    }

    @PutMapping("/me/profile")
    public CurrentUserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateCurrentUserProfile(authentication.getName(), request);
    }

    @GetMapping("/{id}/public-profile")
    public PublicUserProfileResponse publicProfile(@PathVariable Long id) {
        return userService.getPublicProfile(id);
    }
}
