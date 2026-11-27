package com.nagarbondhu.service;

import com.nagarbondhu.dto.request.CreateUserRequest;
import com.nagarbondhu.dto.request.UpdateProfileRequest;
import com.nagarbondhu.dto.request.UserStatusRequest;
import com.nagarbondhu.dto.response.UserResponse;
import com.nagarbondhu.entity.Role;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.RoleName;
import com.nagarbondhu.exception.DuplicateResourceException;
import com.nagarbondhu.exception.ResourceNotFoundException;
import com.nagarbondhu.repository.RefreshTokenRepository;
import com.nagarbondhu.repository.RoleRepository;
import com.nagarbondhu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createUserByAdmin(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already in use: " + email);
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleStr : request.getRoles()) {
                String cleanRole = roleStr.toUpperCase().replace("ROLE_", "").trim();
                RoleName roleName = RoleName.valueOf(cleanRole);
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleStr));
                roles.add(role);
            }
        } else {
            Role buyerRole = roleRepository.findByName(RoleName.BUYER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role BUYER not found"));
            roles.add(buyerRole);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .active(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);
        log.info("Admin created new user with email: {}", email);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setName(request.getName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Email is already in use: " + newEmail);
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleStr : request.getRoles()) {
                String cleanRole = roleStr.toUpperCase().replace("ROLE_", "").trim();
                RoleName roleName = RoleName.valueOf(cleanRole);
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleStr));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(request.getActive());
        user = userRepository.save(user);
        log.info("User id {} active status updated to {}", id, request.getActive());
        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("Deleted user with id: {}", id);
    }

    public UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .active(user.isActive())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
