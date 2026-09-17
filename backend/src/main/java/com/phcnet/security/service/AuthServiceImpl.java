package com.phcnet.security.service;

import com.phcnet.common.exception.BadRequestException;
import com.phcnet.security.dto.AuthResponse;
import com.phcnet.security.dto.DemoAccountDTO;
import com.phcnet.security.dto.LoginRequest;
import com.phcnet.security.dto.UserInfoDTO;
import com.phcnet.security.jwt.JwtTokenProvider;
import com.phcnet.security.model.Role;
import com.phcnet.security.model.User;
import com.phcnet.security.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new BadRequestException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getAssignedDistrict(),
                user.getAssignedPhcId(),
                tokenProvider.getExpirationMs()
        );
    }

    @Override
    public UserInfoDTO getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            throw new BadRequestException("No active authenticated session");
        }

        return new UserInfoDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAssignedDistrict(),
                user.getAssignedPhcId()
        );
    }

    @Override
    public List<DemoAccountDTO> getDemoAccounts() {
        return List.of(
                new DemoAccountDTO(
                        "admin",
                        "admin123",
                        "Dr. Rajesh Verma (State Health Director)",
                        Role.ROLE_ADMIN,
                        "State-level administrator with complete network oversight, model recalibration, and transfer approval authorities across all 5 districts.",
                        null,
                        null
                ),
                new DemoAccountDTO(
                        "district_pune",
                        "pune123",
                        "Dr. Sunita Kulkarni (Pune District Health Officer)",
                        Role.ROLE_DISTRICT_OFFICER,
                        "District-level authority managing supply optimization, inter-facility transfers, and local stock alerts within Pune district.",
                        "Pune",
                        null
                ),
                new DemoAccountDTO(
                        "phc_shirwal",
                        "shirwal123",
                        "Dr. Amit Deshmukh (Medical Officer In-Charge)",
                        Role.ROLE_PHC_MANAGER,
                        "Facility-level manager handling daily OPD dispensing, emergency requisitioning, and local inventory audits at Shirwal PHC-2.",
                        "Satara",
                        1L
                )
        );
    }
}
