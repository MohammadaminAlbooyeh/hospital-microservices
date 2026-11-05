package com.hospital.auth.application;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.RegisterRequest;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.domain.model.Role;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.auth.domain.service.TokenService;
import com.hospital.auth.infrastructure.messaging.AuthEventPublisher;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email is already registered");
        }

        Role role = Role.from(request.role());
        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone() == null ? null : request.phone().trim())
                .roles(role.name())
                .enabled(true)
                .emailVerified(false)
                .build();

        UserAccount saved = userRepository.save(user);
        eventPublisher.userRegistered(saved);
        TokenService.IssuedTokens tokens = tokenService.issue(saved);
        return UserMapper.toAuthResponse(tokens, saved);
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
