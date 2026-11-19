package com.athleet.core.iam.repository;

import com.athleet.core.iam.domain.PasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordTokenRepository extends JpaRepository<PasswordToken, UUID> {
    Optional<PasswordToken> findByTokenHash(String tokenHash);
}
