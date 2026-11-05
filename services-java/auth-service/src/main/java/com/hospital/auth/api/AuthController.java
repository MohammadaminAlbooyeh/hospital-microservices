package com.hospital.auth.api;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.LoginRequest;
import com.hospital.auth.api.dto.LogoutRequest;
import com.hospital.auth.api.dto.OtpRequest;
import com.hospital.auth.api.dto.OtpVerifyRequest;
import com.hospital.auth.api.dto.RefreshTokenRequest;
import com.hospital.auth.api.dto.RegisterRequest;
import com.hospital.auth.api.dto.TokenResponse;
import com.hospital.auth.api.dto.UserProfileResponse;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.application.LoginUseCase;
import com.hospital.auth.application.LogoutUseCase;
import com.hospital.auth.application.RefreshTokenUseCase;
import com.hospital.auth.application.RegisterUserUseCase;
import com.hospital.auth.application.RequestOtpUseCase;
import com.hospital.auth.application.UserQueryService;
import com.hospital.auth.application.VerifyOtpUseCase;
import com.hospital.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final UserQueryService userQueryService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerUserUseCase.execute(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return loginUseCase.execute(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenUseCase.execute(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        logoutUseCase.execute(request);
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest request) {
        requestOtpUseCase.execute(request);
        return ResponseEntity.accepted()
                .body(Map.of("message", "OTP sent", "expiresInSeconds", 300));
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return verifyOtpUseCase.execute(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me() {
        return UserMapper.toProfile(userQueryService.getByEmail(SecurityUtils.requireSubject()));
    }
}
