package com.hospital.auth.application;

import com.hospital.auth.api.dto.OtpRequest;
import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestOtpUseCase {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${hospital.auth.otp-ttl-minutes:5}")
    private long otpTtlMinutes;

    @Transactional
    public void execute(OtpRequest request) {
        String email = RegisterUserUseCase.normalize(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("No account found for this email"));

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setOtpCode(code);
        user.setOtpExpiresAt(Instant.now().plus(otpTtlMinutes, ChronoUnit.MINUTES));
        userRepository.save(user);

        log.info("Generated email verification OTP for {}: {} (valid for {} minutes)",
                user.getEmail(), code, otpTtlMinutes);
    }
}
