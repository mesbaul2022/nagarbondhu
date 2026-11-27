package com.nagarbondhu.config;

import com.nagarbondhu.entity.MaintenanceTeam;
import com.nagarbondhu.entity.Role;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.entity.enums.ComplaintCategory;
import com.nagarbondhu.entity.enums.RoleName;
import com.nagarbondhu.repository.MaintenanceTeamRepository;
import com.nagarbondhu.repository.RoleRepository;
import com.nagarbondhu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MaintenanceTeamRepository maintenanceTeamRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = seedRoleIfMissing(RoleName.ADMIN);
        Role sellerRole = seedRoleIfMissing(RoleName.SELLER);
        Role buyerRole = seedRoleIfMissing(RoleName.BUYER);

        seedUserIfMissing(
                "System Administrator",
                "admin@nagarbondhu.gov.bd",
                "Admin@123",
                "+880-1700-000001",
                adminRole
        );
        User drainageStaff = seedUserIfMissing(
                "Drainage Maintenance Lead",
                "drainage.staff@nagarbondhu.gov.bd",
                "Staff@123",
                "+880-1700-000002",
                sellerRole
        );
        User roadStaff = seedUserIfMissing(
                "Road Maintenance Lead",
                "road.staff@nagarbondhu.gov.bd",
                "Staff@123",
                "+880-1700-000003",
                sellerRole
        );
        seedUserIfMissing(
                "Citizen Reporter",
                "resident@nagarbondhu.gov.bd",
                "Resident@123",
                "+880-1700-000004",
                buyerRole
        );

        seedTeamIfMissing(
                "Drainage Rapid Unit",
                ComplaintCategory.WATERLOGGING,
                "+880-1700-000101",
                "Zone A - Central",
                Set.of(drainageStaff)
        );
        seedTeamIfMissing(
                "Road Repair Squad",
                ComplaintCategory.DAMAGED_ROAD,
                "+880-1700-000102",
                "Zone B - North",
                Set.of(roadStaff)
        );
        seedTeamIfMissing(
                "Streetlight & Waste Team",
                ComplaintCategory.BROKEN_STREETLIGHT,
                "+880-1700-000103",
                "Zone C - South",
                Set.of(roadStaff)
        );

        log.info("DataSeeder verified roles, default accounts, and maintenance teams.");
    }

    private Role seedRoleIfMissing(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private User seedUserIfMissing(String name, String email, String rawPassword, String phone, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .phone(phone)
                        .active(true)
                        .roles(new HashSet<>(Set.of(role)))
                        .build()));
    }

    private void seedTeamIfMissing(
            String teamName,
            ComplaintCategory category,
            String contactPhone,
            String zone,
            Set<User> members) {
        if (!maintenanceTeamRepository.existsByTeamName(teamName)) {
            maintenanceTeamRepository.save(MaintenanceTeam.builder()
                    .teamName(teamName)
                    .category(category)
                    .contactPhone(contactPhone)
                    .zone(zone)
                    .active(true)
                    .members(new HashSet<>(members))
                    .build());
        }
    }
}
