package com.hospital.auth.domain.repository;

import com.hospital.auth.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);
}
