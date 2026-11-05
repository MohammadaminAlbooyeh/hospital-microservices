package com.hospital.auth.application;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.OtpVerifyRequest;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.auth.domain.service.TokenService;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerifyOtpUseCase {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse execute(OtpVerifyRequest request) {
        String email = RegisterUserUseCase.normalize(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("No account found for this email"));

        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) {
            throw ApiException.badRequest("No OTP has been requested for this account");
        }
        if (!user.getOtpExpiresAt().isAfter(Instant.now())) {
            throw ApiException.badRequest("OTP has expired");
        }
        if (!constantTimeEquals(user.getOtpCode(), request.code().trim())) {
            throw ApiException.badRequest("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        UserAccount saved = userRepository.save(user);

        TokenService.IssuedTokens tokens = tokenService.issue(saved);
        return UserMapper.toAuthResponse(tokens, saved);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
