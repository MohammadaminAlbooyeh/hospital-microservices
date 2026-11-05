package com.hospital.auth.infrastructure.persistence;

import com.hospital.auth.domain.model.RefreshToken;
import com.hospital.auth.domain.repository.RefreshTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, UUID>, RefreshTokenRepository {
}
