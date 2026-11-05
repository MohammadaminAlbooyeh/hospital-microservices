package com.hospital.auth.application;

import com.hospital.auth.api.dto.RefreshTokenRequest;
import com.hospital.auth.api.dto.TokenResponse;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.domain.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final TokenService tokenService;

    @Transactional
    public TokenResponse execute(RefreshTokenRequest request) {
        return UserMapper.toTokenResponse(tokenService.rotate(request.refreshToken()));
    }
}
