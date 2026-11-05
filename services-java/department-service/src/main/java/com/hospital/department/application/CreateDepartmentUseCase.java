package com.hospital.department.application;

import com.hospital.department.api.dto.DepartmentRequest;
import com.hospital.department.api.mapper.DepartmentMapper;
import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.service.DepartmentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateDepartmentUseCase {

    private final DepartmentDomainService departmentDomainService;

    @Transactional
    public Department execute(DepartmentRequest request) {
        return departmentDomainService.create(DepartmentMapper.toEntity(request));
    }
}
