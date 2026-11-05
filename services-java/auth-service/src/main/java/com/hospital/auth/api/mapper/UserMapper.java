package com.hospital.auth.api.mapper;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.TokenResponse;
import com.hospital.auth.api.dto.UserProfileResponse;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.service.TokenService;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserProfileResponse toProfile(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.roleList(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }

    public static AuthResponse toAuthResponse(TokenService.IssuedTokens tokens, UserAccount user) {
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.expiresInSeconds(),
                toProfile(user));
    }

    public static TokenResponse toTokenResponse(TokenService.IssuedTokens tokens) {
        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.expiresInSeconds());
    }
}
