package com.santosh.roommate_marketplace_api.auth.service;

import com.santosh.roommate_marketplace_api.auth.dto.AuthResponse;
import com.santosh.roommate_marketplace_api.auth.dto.LoginRequest;
import com.santosh.roommate_marketplace_api.auth.dto.LogoutRequest;
import com.santosh.roommate_marketplace_api.auth.dto.RefreshTokenRequest;
import com.santosh.roommate_marketplace_api.auth.dto.RegisterRequest;
import com.santosh.roommate_marketplace_api.common.exception.BadRequestException;
import com.santosh.roommate_marketplace_api.security.JwtService;
import com.santosh.roommate_marketplace_api.user.dto.CurrentUserResponse;
import com.santosh.roommate_marketplace_api.user.entity.User;
import com.santosh.roommate_marketplace_api.user.entity.UserProfile;
import com.santosh.roommate_marketplace_api.user.entity.UserType;
import com.santosh.roommate_marketplace_api.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        user.setUniversityName(trimToNull(request.universityName()));
        user.setBio(trimToNull(request.bio()));
        user.setGender(trimToNull(request.gender()));
        user.setUserType(request.userType() == null ? UserType.OTHER : request.userType());

        UserProfile profile = new UserProfile();
        profile.setDisplayName(user.getFirstName() + " " + user.getLastName());
        profile.setUniversity(user.getUniversityName());
        profile.setBio(user.getBio());
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndRotate(request.refreshToken());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                CurrentUserResponse.from(user)
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
