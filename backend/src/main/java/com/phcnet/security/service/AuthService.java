package com.phcnet.security.service;

import com.phcnet.security.dto.AuthResponse;
import com.phcnet.security.dto.DemoAccountDTO;
import com.phcnet.security.dto.LoginRequest;
import com.phcnet.security.dto.UserInfoDTO;

import java.util.List;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserInfoDTO getCurrentUser();
    List<DemoAccountDTO> getDemoAccounts();
}
