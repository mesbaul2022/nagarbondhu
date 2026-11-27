package com.nagarbondhu.repository;

import com.nagarbondhu.entity.Complaint;
import com.nagarbondhu.entity.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByTrackingNo(String trackingNo);

    List<Complaint> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Complaint> findByAssignedTeamIdInOrderByCreatedAtDesc(Collection<Long> teamIds);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    long countByStatus(ComplaintStatus status);

    @Query("SELECT c.trackingNo FROM Complaint c")
    List<String> findAllTrackingNumbers();
}
