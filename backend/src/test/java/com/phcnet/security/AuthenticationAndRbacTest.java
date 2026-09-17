package com.phcnet.security;

import com.phcnet.security.dto.AuthResponse;
import com.phcnet.security.dto.DemoAccountDTO;
import com.phcnet.security.dto.LoginRequest;
import com.phcnet.security.jwt.JwtTokenProvider;
import com.phcnet.security.model.Role;
import com.phcnet.security.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthenticationAndRbacTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testAdminAuthenticationAndTokenGeneration() {
        LoginRequest req = new LoginRequest("admin", "admin123");
        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("admin", response.username());
        assertEquals(Role.ROLE_ADMIN, response.role());

        // Validate JWT signature and claims
        assertTrue(jwtTokenProvider.validateToken(response.token()));
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(response.token()));
    }

    @Test
    void testDistrictOfficerAuthentication() {
        LoginRequest req = new LoginRequest("district_pune", "pune123");
        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals(Role.ROLE_DISTRICT_OFFICER, response.role());
        assertEquals("Pune", response.assignedDistrict());
        assertTrue(jwtTokenProvider.validateToken(response.token()));
    }

    @Test
    void testPhcManagerAuthentication() {
        LoginRequest req = new LoginRequest("phc_shirwal", "shirwal123");
        AuthResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals(Role.ROLE_PHC_MANAGER, response.role());
        assertEquals("Satara", response.assignedDistrict());
        assertNotNull(response.assignedPhcId());
    }

    @Test
    void testInvalidCredentialsRejected() {
        LoginRequest badPassword = new LoginRequest("admin", "wrongpassword");
        assertThrows(RuntimeException.class, () -> authService.login(badPassword));

        LoginRequest badUser = new LoginRequest("nonexistent", "admin123");
        assertThrows(RuntimeException.class, () -> authService.login(badUser));
    }

    @Test
    void testDemoAccountsList() {
        List<DemoAccountDTO> demos = authService.getDemoAccounts();
        assertNotNull(demos);
        assertEquals(3, demos.size());

        boolean hasAdmin = demos.stream().anyMatch(d -> d.role() == Role.ROLE_ADMIN);
        boolean hasDho = demos.stream().anyMatch(d -> d.role() == Role.ROLE_DISTRICT_OFFICER);
        boolean hasMo = demos.stream().anyMatch(d -> d.role() == Role.ROLE_PHC_MANAGER);

        assertTrue(hasAdmin);
        assertTrue(hasDho);
        assertTrue(hasMo);
    }
}
