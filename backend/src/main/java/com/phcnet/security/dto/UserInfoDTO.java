package com.phcnet.security.dto;

import com.phcnet.security.model.Role;

public record UserInfoDTO(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        String assignedDistrict,
        Long assignedPhcId
) {}
