package com.santosh.roommate_marketplace_api.auth.email.repository;

import com.santosh.roommate_marketplace_api.auth.email.entity.EmailVerificationToken;
import com.santosh.roommate_marketplace_api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByUserAndUsedAtIsNull(User user);
}
