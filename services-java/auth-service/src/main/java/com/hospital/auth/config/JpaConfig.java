package com.hospital.auth.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"com.hospital.auth", "com.hospital.shared"})
@EntityScan(basePackages = {"com.hospital.auth", "com.hospital.shared"})
public class JpaConfig {
}
