package com.nagarbondhu.repository;

import com.nagarbondhu.entity.Role;
import com.nagarbondhu.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
