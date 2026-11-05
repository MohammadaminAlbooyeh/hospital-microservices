package com.hospital.auth.domain.repository;

import com.hospital.auth.domain.model.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    UserAccount save(UserAccount user);

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByEmail(String email);

    List<UserAccount> findAll();

    boolean existsByEmail(String email);
}
