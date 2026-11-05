package com.hospital.auth.unit;

import com.hospital.auth.api.dto.RegisterRequest;
import com.hospital.auth.application.RegisterUserUseCase;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.auth.domain.service.TokenService;
import com.hospital.auth.infrastructure.messaging.AuthEventPublisher;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthEventPublisher eventPublisher;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "jane@example.com", "Secret#123", "Jane", "Doe", "+100000000", "PATIENT");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.execute(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(409))
                .hasMessageContaining("already registered");

        verifyNoInteractions(passwordEncoder, tokenService, eventPublisher);
    }
}
