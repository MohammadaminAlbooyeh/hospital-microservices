package com.hospital.auth.api;

import com.hospital.auth.api.dto.UserProfileResponse;
import com.hospital.auth.api.mapper.UserMapper;
import com.hospital.auth.application.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserQueryService userQueryService;

    @GetMapping("/{id}")
    public UserProfileResponse get(@PathVariable UUID id) {
        return UserMapper.toProfile(userQueryService.getById(id));
    }

    @GetMapping
    public List<UserProfileResponse> list() {
        return userQueryService.list().stream().map(UserMapper::toProfile).toList();
    }
}
