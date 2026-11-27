package com.nagarbondhu.service;

import com.nagarbondhu.dto.request.LoginRequest;
import com.nagarbondhu.dto.request.RefreshTokenRequest;
import com.nagarbondhu.dto.request.RegisterRequest;
import com.nagarbondhu.dto.response.AuthResponse;
import com.nagarbondhu.entity.RefreshToken;
import com.nagarbondhu.entity.Role;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.RoleName;
import com.nagarbondhu.exception.DuplicateResourceException;
import com.nagarbondhu.exception.ResourceNotFoundException;
import com.nagarbondhu.repository.RoleRepository;
import com.nagarbondhu.repository.UserRepository;
import com.nagarbondhu.security.JwtService;
import com.nagarbondhu.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use: " + email);
        }

        Role buyerRole = roleRepository.findByName(RoleName.BUYER)
                .orElseThrow(() -> new ResourceNotFoundException("Role BUYER not found"));

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .active(true)
                .roles(new HashSet<>(Set.of(buyerRole)))
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        if (!user.isActive()) {
            throw new DisabledException("User account is deactivated");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = oldToken.getUser();

        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        String newAccessToken = jwtService.generateToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
        SecurityContextHolder.clearContext();
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }
}
