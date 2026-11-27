package com.nagarbondhu.repository;

import com.nagarbondhu.entity.MaintenanceTeam;
import com.nagarbondhu.entity.enums.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceTeamRepository extends JpaRepository<MaintenanceTeam, Long> {
    boolean existsByTeamName(String teamName);

    List<MaintenanceTeam> findByCategoryAndActiveTrue(ComplaintCategory category);

    List<MaintenanceTeam> findByMembersId(Long userId);
}
