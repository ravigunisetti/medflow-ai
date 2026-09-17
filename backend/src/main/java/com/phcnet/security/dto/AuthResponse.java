package com.phcnet.security.dto;

import com.phcnet.security.model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String fullName,
        Role role,
        String assignedDistrict,
        Long assignedPhcId,
        long expiresInMs
) {}
