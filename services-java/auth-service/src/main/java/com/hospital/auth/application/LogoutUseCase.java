package com.hospital.auth.application;

import com.hospital.auth.api.dto.LogoutRequest;
import com.hospital.auth.domain.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final TokenService tokenService;

    @Transactional
    public void execute(LogoutRequest request) {
        tokenService.revoke(request.refreshToken());
    }
}
