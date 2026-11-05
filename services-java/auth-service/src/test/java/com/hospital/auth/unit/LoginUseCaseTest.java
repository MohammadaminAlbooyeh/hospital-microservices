package com.hospital.auth.unit;

import com.hospital.auth.api.dto.AuthResponse;
import com.hospital.auth.api.dto.LoginRequest;
import com.hospital.auth.application.LoginUseCase;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.auth.domain.service.TokenService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    void issuesAccessAndRefreshTokensForValidCredentials() {
        UserAccount user = activeUser();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret#123", "bcrypt-hash")).thenReturn(true);
        when(tokenService.issue(user))
                .thenReturn(new TokenService.IssuedTokens("access-token", "refresh-token", 900));

        AuthResponse response = loginUseCase.execute(new LoginRequest("jane@example.com", "Secret#123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        assertThat(response.user().roles()).containsExactly("PATIENT");
    }

    @Test
    void rejectsInvalidPassword() {
        UserAccount user = activeUser();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.execute(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");
    }

    private UserAccount activeUser() {
        UserAccount user = UserAccount.builder()
                .email("jane@example.com")
                .passwordHash("bcrypt-hash")
                .firstName("Jane")
                .lastName("Doe")
                .phone("+100000000")
                .roles("PATIENT")
                .enabled(true)
                .emailVerified(true)
                .build();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
