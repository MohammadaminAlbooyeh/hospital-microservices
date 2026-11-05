package com.hospital.auth.infrastructure.persistence;

import com.hospital.auth.domain.model.UserAccount;
import com.hospital.auth.domain.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserAccount, UUID>, UserRepository {
}
