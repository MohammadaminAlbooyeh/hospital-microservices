package com.hospital.auth.domain.service;

import com.hospital.auth.domain.model.RefreshToken;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.RefreshTokenRepository;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.shared.exception.ApiException;
import com.hospital.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public IssuedTokens issue(UserAccount user) {
        if (user.getId() == null) {
            throw ApiException.badRequest("User must be persisted before issuing tokens");
        }
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("roles", String.join(",", user.roleList()));
        claims.put("email", user.getEmail());
        claims.put("userId", user.getId().toString());
        claims.put("name", fullName(user));

        String accessToken = jwtService.createAccessToken(user.getEmail(), claims);
        String refreshToken = jwtService.createRefreshToken(user.getEmail());

        RefreshToken stored = RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiresAt(expiryOf(refreshToken))
                .revoked(false)
                .build();
        refreshTokenRepository.save(stored);

        return new IssuedTokens(accessToken, refreshToken, jwtService.getAccessTtlMinutes() * 60);
    }

    @Transactional
    public IssuedTokens rotate(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw ApiException.unauthorized("Refresh token has been revoked");
        }
        if (stored.getExpiresAt() == null || !stored.getExpiresAt().isAfter(Instant.now())) {
            throw ApiException.unauthorized("Refresh token has expired");
        }

        try {
            Claims claims = jwtService.parse(token);
            if (!"refresh".equals(claims.get("type"))) {
                throw ApiException.unauthorized("Token is not a refresh token");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.unauthorized("Invalid refresh token");
        }

        UserAccount user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Refresh token owner no longer exists"));
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Account is disabled");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issue(user);
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(stored -> {
            if (!stored.isRevoked()) {
                stored.setRevoked(true);
                refreshTokenRepository.save(stored);
            }
        });
    }

    private Instant expiryOf(String token) {
        try {
            return jwtService.parse(token).getExpiration().toInstant();
        } catch (Exception ex) {
            throw ApiException.unauthorized("Unable to issue refresh token");
        }
    }

    private String fullName(UserAccount user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    public record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }
}
