package com.phcnet.security.controller;

import com.phcnet.common.dto.ApiResponse;
import com.phcnet.security.dto.AuthResponse;
import com.phcnet.security.dto.DemoAccountDTO;
import com.phcnet.security.dto.LoginRequest;
import com.phcnet.security.dto.UserInfoDTO;
import com.phcnet.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & RBAC", description = "User authentication, JWT generation, and role identification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue signed Bearer JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Fetch current authenticated user profile")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getCurrentUser() {
        UserInfoDTO userInfo = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }

    @GetMapping("/demo-accounts")
    @Operation(summary = "List pre-configured test credentials for evaluators and hackathon judges")
    public ResponseEntity<ApiResponse<List<DemoAccountDTO>>> getDemoAccounts() {
        List<DemoAccountDTO> accounts = authService.getDemoAccounts();
        return ResponseEntity.ok(ApiResponse.ok("Demo accounts retrieved", accounts));
    }
}
