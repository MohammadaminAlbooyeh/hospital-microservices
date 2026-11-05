package com.hospital.auth.application;

import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserAccount getByEmail(String email) {
        return userRepository.findByEmail(RegisterUserUseCase.normalize(email))
                .orElseThrow(() -> ApiException.notFound("User account not found"));
    }

    @Transactional(readOnly = true)
    public UserAccount getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User account not found"));
    }

    @Transactional(readOnly = true)
    public List<UserAccount> list() {
        return userRepository.findAll();
    }
}
