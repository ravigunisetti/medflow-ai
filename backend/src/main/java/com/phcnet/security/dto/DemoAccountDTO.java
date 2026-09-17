package com.phcnet.security.dto;

import com.phcnet.security.model.Role;

public record DemoAccountDTO(
        String username,
        String password,
        String fullName,
        Role role,
        String description,
        String assignedDistrict,
        Long assignedPhcId
) {}
