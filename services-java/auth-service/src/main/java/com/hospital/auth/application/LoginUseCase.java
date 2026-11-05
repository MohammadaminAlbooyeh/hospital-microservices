package com.hospital.auth.application;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.LoginRequest;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.auth.domain.service.TokenService;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        String email = RegisterUserUseCase.normalize(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Account is disabled");
        }

        TokenService.IssuedTokens tokens = tokenService.issue(user);
        return UserMapper.toAuthResponse(tokens, user);
    }
}
